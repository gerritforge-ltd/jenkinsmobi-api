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

package mobi.jenkinsci.ci.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;


public abstract class AbstractSecureHttpClient {

  protected HttpClient wrappedDefaultHttpClient;
  protected String domainName;
  protected int port;
  protected String protocol;
  protected String fullUrl;
  protected String queryPath;
  protected HttpCredentials credentials;
  protected boolean performAuthentication = true;
  protected HttpUriRequest lastRequest;
  protected boolean connectionAborted = false;
  protected String customUserAgentInfo = null;
  private final JenkinsConfig config;

  public AbstractSecureHttpClient(final JenkinsConfig config,
      HttpClient httpClient, String url, HttpCredentials credentials,
      String customUserAgentExtention) {
    this.config = config;
    this.wrappedDefaultHttpClient = httpClient;
    if (customUserAgentExtention != null) {
      wrappedDefaultHttpClient.getParams().setParameter(
          CoreProtocolPNames.USER_AGENT,
          customUserAgentExtention);
    }
    this.credentials = credentials;
    this.fullUrl = url;

    this.port = UrlParser.getPort(url);
    this.protocol = UrlParser.getProtocol(url);
    this.domainName = UrlParser.getDomainName(url);
    this.queryPath = UrlParser.getQueryPath(url);

    if (credentials.getPassword() == null
        || credentials.getPassword().length() == 0) {

      performAuthentication = false;
    }
  }

  public void abortConnection() {

    if (lastRequest != null) {

      connectionAborted = true;
      lastRequest.abort();
    }
  }

  protected HttpResponse executeQuery(HttpUriRequest request)
      throws ClientProtocolException, IOException {
    lastRequest = request;
    return wrappedDefaultHttpClient.execute(request);
  }

  public HttpResponse executeGetQuery() throws ClientProtocolException,
      IOException {

    return executeGetQuery(null);
  }

  public HttpResponse executeGetQuery(Map<String, String> headers)
      throws ClientProtocolException, IOException {

    HttpGet get = new HttpGet(fullUrl);

    setGetHeaders(headers, get);

    return executeQuery(get);
  }

  public HttpResponse executePostQuery() throws ClientProtocolException,
      IOException {

    HttpPost post = new HttpPost(fullUrl);

    return executeQuery(post);
  }

  public HttpResponse executePostQuery(Map<String, String> parameters)
      throws ClientProtocolException, IOException {

    HttpPost post = new HttpPost(fullUrl);

    setPostParameters(parameters, post);

    return executeQuery(post);
  }

  public HttpResponse executePostQuery(Map<String, String> parameters,
      Map<String, String> headers) throws ClientProtocolException, IOException {

    HttpPost post = new HttpPost(fullUrl);

    setPostHeaders(headers, post);

    setPostParameters(parameters, post);

    return executeQuery(post);
  }

  public boolean isConnectionAborted() {

    return connectionAborted;
  }

  public void setPostParameters(Map<String, String> parameters, HttpPost post)
      throws UnsupportedEncodingException {
    if (parameters != null && parameters.size() > 0) {
      Iterator<String> keysIter = parameters.keySet().iterator();
      List<NameValuePair> nameValuePairList = new LinkedList<NameValuePair>();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = parameters.get(key);
        nameValuePairList.add(new BasicNameValuePair(key, value));
      }

      post.setEntity(new UrlEncodedFormEntity(nameValuePairList, "UTF-8"));
    }
  }

  public void setPostHeaders(Map<String, String> headers, HttpPost post)
      throws UnsupportedEncodingException {
    if (headers != null && headers.size() > 0) {
      Iterator<String> keysIter = headers.keySet().iterator();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = headers.get(key);

        post.setHeader(key, value);
      }
    }

    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
  }

  public void setGetHeaders(Map<String, String> headers, HttpGet get)
      throws UnsupportedEncodingException {
    if (headers != null && headers.size() > 0) {
      Iterator<String> keysIter = headers.keySet().iterator();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = headers.get(key);

        get.setHeader(key, value);
      }
    }
  }
}
