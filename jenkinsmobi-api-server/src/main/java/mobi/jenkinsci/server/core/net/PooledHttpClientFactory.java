// Copyright (C) 2013 GerritForge www.gerritforge.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package mobi.jenkinsci.server.core.net;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.server.Config;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class PooledHttpClientFactory implements HttpClientFactory {

  private final Config config;
  private final HttpClient staticHttpClient;
  private final HashMap<String, HttpClient> staticAuthHttpClients;

  @Inject
  public PooledHttpClientFactory(Config config) {
    this.config = config;
    staticHttpClient = getCachingHttpClient();
    staticAuthHttpClients = Maps.newHashMap();
  }

  private HttpClientBuilder configureHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
    return httpClientBuilder.setConnectionManager(getHttpConnectionManager())
            .setDefaultRequestConfig(getRequestConfig()).setDefaultSocketConfig(getSocketConfig());
  }

  private SocketConfig getSocketConfig() {
    return SocketConfig.custom().setSoReuseAddress(true).setSoTimeout(config.getHttpReadTimeoutMsec()).build();
  }

  private RequestConfig getRequestConfig() {
    return RequestConfig.custom().setConnectTimeout(config.getHttpConnectionTimeoutMsec())
            .setConnectionRequestTimeout(config.getHttpConnectionTimeoutMsec()).setCircularRedirectsAllowed(false)
            .setRedirectsEnabled(true).setSocketTimeout(config.getHttpReadTimeoutMsec())
            .setStaleConnectionCheckEnabled(true).build();
  }

  private HttpClient getCachingHttpClient() {
    return configureHttpClientBuilder(configureHttpCacheClientBuilder(CachingHttpClientBuilder.create())).build();
  }

  private CachingHttpClientBuilder configureHttpCacheClientBuilder(CachingHttpClientBuilder httpClientBuilder) {
    return httpClientBuilder.setCacheConfig(getCacheConfig());
  }

  private CacheConfig getCacheConfig() {
    return CacheConfig.custom().setMaxObjectSize(config.getHttpCacheMaxObjectSize())
            .setMaxCacheEntries(config.getHttpCacheMaxObjects()).setHeuristicCachingEnabled(true).build();
  }

  private PoolingHttpClientConnectionManager getHttpConnectionManager() {
    final PoolingHttpClientConnectionManager httpConnManager = new PoolingHttpClientConnectionManager();
    httpConnManager.setMaxTotal(config.getHttpMaxConnections());
    return httpConnManager;
  }

  @Override
  public HttpClient getHttpClient() {
    return staticHttpClient;
  }

  @Override
  public synchronized HttpClient getBasicAuthHttpClient(final URL url, final String user, final String password) throws MalformedURLException {
    final String clientKey = user + ":" + password + "@" + url;
    HttpClient client = staticAuthHttpClients.get(clientKey);
    if (client == null) {
      client = getNewBasicAuthHttpClient(url, user, password);
      staticAuthHttpClients.put(clientKey, client);
    }

    return client;
  }

  private HttpClient getNewBasicAuthHttpClient(final URL url, final String user, final String password) {
    return configureHttpClientBuilder(configureHttpCacheClientBuilder(CachingHttpClientBuilder.create()))
            .setDefaultCredentialsProvider(getCredentialsProvider(url, user, password)).build();
  }

  private CredentialsProvider getCredentialsProvider(final URL url, final String user, final String password) {
    CredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(new AuthScope(url.getHost(), url
            .getPort(), AuthScope.ANY_REALM, "BASIC"), new UsernamePasswordCredentials(user, password));
    return provider;
  }

}
