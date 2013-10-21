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
package mobi.jenkinsci.server.core.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Config;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.net.ProxyUtil;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.URLDownloader;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ResourceService implements URLDownloader {
  private static final Logger log = Logger.getLogger(ResourceService.class);
  private static final String canonicalUrl = System.getProperty("canonicalUrl");

  @Inject
  private ProxyUtil proxyUtil;

  @Inject
  private Config config;

  @Inject
  private HttpClientFactory httpClientFactory;

  @Override
  public RawBinaryNode internalQueryForDownload(final HttpServletRequest req,
      final String url, final Account account, final PluginConfig pluginConfig)
      throws IOException {

    final HttpClient client = httpClientFactory.getHttpClient();
    final HttpGet get = new HttpGet(url);
    final HttpResponse response = client.execute(get);

    final StatusLine status = response.getStatusLine();
    if (status.getStatusCode() != HttpURLConnection.HTTP_OK) {
      throw new IOException("HTTP-GET " + url + " failed: status code "
          + status);
    }

    final RawBinaryNode result = new RawBinaryNode();
    result.setData(response.getEntity().getContent());

    for (final Header h : response.getAllHeaders()) {
      if (h.getName().equals(HttpHeaders.CONTENT_TYPE)) {
        result.setHttpContentType(h.getValue());
        break;
      } else if (h.getName().equals(HttpHeaders.CONTENT_LENGTH)) {
        result.setSize(Long.parseLong(h.getValue()));
        break;
      }
    }

    return result;
  }

  public AbstractNode getProxiedResourceFile(final HttpServletRequest req)
      throws IOException {
    return getProxiedResourceFile(null, req);
  }

  protected AbstractNode getProxiedResourceFile(final String pluginName,
      final HttpServletRequest req) throws IOException {
    final String reqContext = req.getContextPath();
    String requestURL = req.getRequestURL().toString();

    if (canonicalUrl != null) {
      final URL origUrl = new URL(requestURL);
      requestURL =
          canonicalUrl + origUrl.getPath().substring(reqContext.length());
    }

    RawBinaryNode result = null;
    log.debug("Original URL: '" + requestURL + "'" + " Query string: '"
        + req.getQueryString() + "'");
    final String url = req.getParameter("web");
    final String decorate = req.getParameter("decorate");
    log.debug("web=" + url);
    log.debug("decorate=" + decorate);

    if (url == null) {
      return null;
    }

    result = internalQueryForDownload(req, url, null, null);
    if (result == null) {
      return null;
    }

    if (result.getHttpContentType().contains("text/html")) { // fix links only
                                                             // in
      // text-type content
      final String resultString =
          proxyUtil.proxyfy(req.getHeader("User-Agent"), pluginName, url,
              requestURL.toString(), decorate,
              result.getDownloadedObjectData(), null);

      result.setData(resultString.getBytes());
    }

    return result;
  }

  public String validateConfig(final HttpServletRequest req, final Account account,
      final PluginConfig pluginConf) throws TwoPhaseAuthenticationRequiredException {
    return null;
  }

  public List<ItemNode> getReleaseNotes(final Account account,
      final PluginConfig pluginConf, final String version, final String url,
      final HttpServletRequest request) throws Exception {
    return Collections.emptyList();
  }
}
