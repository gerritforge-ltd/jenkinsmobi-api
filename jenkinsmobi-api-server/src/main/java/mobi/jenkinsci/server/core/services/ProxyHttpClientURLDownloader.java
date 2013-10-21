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
package mobi.jenkinsci.server.core.services;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.URLDownloader;
import mobi.jenkinsci.server.core.module.CanonicalWebUrl;
import mobi.jenkinsci.server.core.net.HttpClientURLDownloader;
import mobi.jenkinsci.server.core.net.ProxyUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class ProxyHttpClientURLDownloader implements URLDownloader {
  private final String canonicalUrl;
  private final ProxyUtil proxyUtil;
  private final HttpClientURLDownloader httpClientDownloader;

  @Inject
  public ProxyHttpClientURLDownloader(ProxyUtil proxyUtil, HttpClientURLDownloader httpClientDownloader,
                                      @CanonicalWebUrl String canonicalUrl) {
    this.httpClientDownloader = httpClientDownloader;
    this.proxyUtil = proxyUtil;
    this.canonicalUrl = canonicalUrl;
  }

  @Override
  public RawBinaryNode download(HttpServletRequest req, String targetDownloadUrl, Account account,
                                PluginConfig pluginConfig) throws IOException {
    final String downloadUrl = getTargetDownloadURLString(req, targetDownloadUrl);

    final RawBinaryNode result = httpClientDownloader.download(req, downloadUrl, account, pluginConfig);

    if (isHTMLPage(result)) {
      String resultData = proxyUtil.proxyfy(req.getHeader("User-Agent"), pluginConfig.getName(), downloadUrl,
              result.getDownloadedObjectData());
      resultData = proxyUtil.rewriteLinks(downloadUrl, getRequestURLString(req), resultData);
      if (!Strings.isNullOrEmpty(req.getParameter("decorate"))) {
        resultData = proxyUtil.wrapInHtmlPage(resultData);
      }
      result.setData(resultData);
    }

    return result;
  }

  private boolean isHTMLPage(RawBinaryNode result) {
    return Strings.nullToEmpty(result.getHttpContentType()).contains("text/html");
  }

  private String getTargetDownloadURLString(HttpServletRequest req, String targetDownloadUrl) {
    return Objects.firstNonNull(req.getParameter("web"), targetDownloadUrl);
  }

  private String getRequestURLString(HttpServletRequest req) throws MalformedURLException {
    StringBuffer requestURL = req.getRequestURL();
    if (canonicalUrl == null) {
      return requestURL == null ? null : requestURL.toString();
    }

    final URL origUrl = new URL(requestURL.toString());
    String contextPath = req.getContextPath();
    String resourcePath = origUrl.getPath().substring(contextPath.length());
    return dropTrailingSlash(canonicalUrl) + resourcePath;
  }

  private String dropTrailingSlash(String canonicalUrl) {
    return canonicalUrl.endsWith("/") ? canonicalUrl.substring(0, canonicalUrl.length() - 1) : canonicalUrl;
  }
}
