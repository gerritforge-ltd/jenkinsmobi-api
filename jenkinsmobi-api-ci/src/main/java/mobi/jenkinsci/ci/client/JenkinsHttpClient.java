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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import mobi.jenkinsci.commons.Constants;

import mobi.jenkinsci.net.HttpClientFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class JenkinsHttpClient {
  private final Logger LOG = Logger.getLogger(JenkinsHttpClient.class);

  private HttpClient httpClient;
  private boolean checkOnlyStatusCode;
  private int lastHttpStatusCode;
  private String lastHttpStatusMsg;
  private Header[] lastHttpHeaders;
  private BasicHttpContext httpContext;
  private final JenkinsConfig config;

  @Inject
  private HttpClientFactory httpClientFactory;

  public JenkinsHttpClient(final JenkinsConfig config) throws MalformedURLException {
    this.config = config;
    if (config.getUsername() == null) {
      httpClient = httpClientFactory.getHttpClient();
    } else {
      final URL url = new URL(config.getUrl());
      httpClient =
          httpClientFactory.getBasicAuthHttpClient(url,
              config.getUsername(), config.getPassword());

      final AuthCache authCache = new BasicAuthCache();
      final BasicScheme basicAuth = new BasicScheme();
      authCache.put(new HttpHost(url.getHost(), url.getPort()), basicAuth);

      httpContext = new BasicHttpContext();
      httpContext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    }
  }

  public InputStream getInputStream(final HttpRequestBase req)
      throws IOException {
    final HttpResponse response = execute(req);
    final int result = elaborateResponse(response).getStatusLine().getStatusCode();

    if (result != HttpStatusCode.HTTP_OK) {
      throw new IOException(response.getStatusLine().toString());
    }

    return response.getEntity().getContent();
  }

  public HttpResponse execute(final HttpRequestBase req) throws IOException {
    LOG.debug("Executing '" + req.getMethod() + " " + req.getURI() + "'");

    if (!(httpClient instanceof JenkinsFormAuthHttpClient)
        && config.getUsername() != null
        && config.getUsername().trim().length() > 0) {
      ensurePreemptiveAuthRequest(req);
    }

    HttpResponse response =
        httpContext == null ?
        httpClient.execute(req):
        httpClient.execute(req, httpContext);
    if (response == null) {
      throw new IOException("Cannot contact URL " + req.getURI());
    }

    final int responseStatus = response.getStatusLine().getStatusCode();
    if ((responseStatus == HttpURLConnection.HTTP_UNAUTHORIZED || responseStatus == HttpURLConnection.HTTP_FORBIDDEN)) {
      req.releaseConnection();

      httpClient =
          new JenkinsFormAuthHttpClient(httpClientFactory.getHttpClient(),
              config.getUrl(), config.getUsername(), config.getPassword(),
              req.getFirstHeader(Constants.X_AUTH_OTP_HEADER) != null ? req
                  .getFirstHeader(Constants.X_AUTH_OTP_HEADER).getValue() : null);
      response = httpClient.execute(req);
      httpContext = null;
    }

    return elaborateResponse(response);
  }

  private void ensurePreemptiveAuthRequest(final HttpRequestBase req) {
    final String username = config.getUsername();
    final String password = config.getPassword();
    final URI reqUri = req.getURI();
    final String reqUriString = reqUri.toString();
    if (reqUriString.indexOf(username) < 0) {
      try {
        final String newUriString =
            reqUriString.replaceAll(
                "://",
                "://" + URLEncoder.encode(username, "UTF-8") + ":"
                    + URLEncoder.encode(password, "UTF-8") + "@");
        req.setURI(new URI(newUriString));
      } catch (final Exception e) {
        LOG.error("Cannot insert user into URI " + reqUriString, e);
        return;
      }
    }
  }

  private HttpResponse elaborateResponse(final HttpResponse response)
      throws IOException {

    getHttpResult(response);
    LOG.debug("Response string: '" + response.getStatusLine().toString()
        + "'");

    return response;
  }

  private void getHttpResult(final HttpResponse response) {
    lastHttpStatusCode = response.getStatusLine().getStatusCode();
    lastHttpStatusMsg = response.getStatusLine().getReasonPhrase();
  }

  public boolean isCheckOnlyStatusCode() {
    return checkOnlyStatusCode;
  }

  public void setCheckOnlyStatusCode(final boolean checkOnlyStatusCode) {
    this.checkOnlyStatusCode = checkOnlyStatusCode;
  }

  public int getLastHttpStatusCode() {
    return lastHttpStatusCode;
  }

  public String getLastHttpStatusMsg() {
    return lastHttpStatusMsg;
  }

  public Header[] getLastHttpHeaders() {
    return lastHttpHeaders;
  }
}
