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
package mobi.jenkinsci.net;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import mobi.jenkinsci.commons.Config;

import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.google.inject.Inject;

public class HttpClientFactory {

  @Inject
  private Config config;

  private final CachingHttpClient staticHttpClient =
      getCachingHttpClient(getDefaultHttpClient());
  private final HashMap<String, HttpClient> staticAuthHttpClients = new HashMap<String, HttpClient>();

  private DefaultHttpClient getDefaultHttpClient() {
    final HttpParams params = new BasicHttpParams();
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
        config.getHttpConnectionTimeoutMsec());
    params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
        config.getHttpReadTimeoutMsec());
    params.setLongParameter("http.conn-manager.timeout",
        config.getHttpConnectionTimeoutMsec());
    final PoolingClientConnectionManager httpConnManager = getHttpConnectionManager();
    return new DefaultHttpClient(httpConnManager, params);
  }


  private CachingHttpClient getCachingHttpClient(final HttpClient client) {
    final CacheConfig httpCacheConfig = new CacheConfig();
    httpCacheConfig.setMaxObjectSize(config.getHttpCacheMaxObjectSize());
    httpCacheConfig.setMaxCacheEntries(config.getHttpCacheMaxObjects());
    httpCacheConfig.setHeuristicCachingEnabled(true);
    return new CachingHttpClient(client, httpCacheConfig);
  }

  public PoolingClientConnectionManager getHttpConnectionManager() {
    final PoolingClientConnectionManager httpConnManager =
        new PoolingClientConnectionManager();
    httpConnManager.setMaxTotal(config.getHttpMaxConnections());
    return httpConnManager;
  }

  public HttpClient getHttpClient() {
    return staticHttpClient;
  }

  public synchronized HttpClient getBasicAuthHttpClient(final URL url, final String user,
      final String password) throws MalformedURLException {
    final String clientKey = user + ":" + password + "@" + url;
    HttpClient client = staticAuthHttpClients.get(clientKey);
    if(client == null) {
      client = getNewBasicAuthHttpClient(url, user, password);
      staticAuthHttpClients.put(clientKey, client);
    }

    return client;
  }

  private HttpClient getNewBasicAuthHttpClient(final URL url, final String user,
      final String password) {
    final DefaultHttpClient client = getDefaultHttpClient();

    client.getCredentialsProvider().setCredentials(
        new AuthScope(url.getHost(), url.getPort()),
        new UsernamePasswordCredentials(user, password));

    return getCachingHttpClient(client);
  }

}
