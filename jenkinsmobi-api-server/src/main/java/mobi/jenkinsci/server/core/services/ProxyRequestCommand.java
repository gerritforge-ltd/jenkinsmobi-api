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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.URLDownloader;

import com.google.inject.Inject;
import mobi.jenkinsci.server.Config;
import mobi.jenkinsci.server.core.net.HttpClientURLDownloader;
import mobi.jenkinsci.server.core.net.ProxyUtil;

@Slf4j
public class ProxyRequestCommand implements RequestCommand {
  private final URLDownloader urlDownloader;
  private final ProxyUtil proxyUtil;
  private final Config config;

  @Inject
  public ProxyRequestCommand(final HttpClientURLDownloader urlDownloader, final ProxyUtil proxyUtil, final Config config) {
    this.urlDownloader = urlDownloader;
    this.proxyUtil = proxyUtil;
    this.config = config;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return getTargetDownloadUrl(request) != null;
  }

  private String getTargetDownloadUrl(final HttpServletRequest request) {
    return request.getParameter("web");
  }

  @Override
  public AbstractNode process(final Account account, final HttpServletRequest req) throws IOException {

    final String url = getTargetDownloadUrl(req);
    if (url == null) {
      throw new ResourceNotFoundException("Cannot find web parameter pointing to the target URL to proxyfy");
    }
    final String requestURL = getCanonicalRequestURL(req);
    log.debug("Proxying resource from target URL=" + requestURL);

    final String pluginName = new UrlPath(req).getPluginId();
    final RawBinaryNode result = urlDownloader.download(req, url, account, account.getPluginConfig(pluginName));

    if (isResultToBeProxied(result)) {
      String resultString = proxyUtil
              .proxyfy(req.getHeader("User-Agent"), pluginName, url , result
                      .getDownloadedObjectData());
      resultString = proxyUtil.rewriteLinks(url, requestURL.toString(), resultString);
      if (!Strings.isNullOrEmpty(req.getParameter("decorate"))) {
        resultString = proxyUtil.wrapInHtmlPage(resultString);
      }

      result.setData(resultString);
    }

    return result;
  }

  private boolean isResultToBeProxied(final RawBinaryNode result) {
    final String httpContentType = result.getHttpContentType();
    return httpContentType != null && httpContentType.contains("text/html");
  }


  private String getCanonicalRequestURL(final HttpServletRequest req) throws MalformedURLException {
    String requestURL = req.getRequestURL().toString();
    if (config.getCanonicalUrl() != null) {
      final String origUrlPath = new URL(requestURL).getPath();
      final String reqContextPath = req.getContextPath();
      requestURL = config.getCanonicalUrl() + origUrlPath.substring(reqContextPath.length());
    }
    return requestURL;
  }
}
