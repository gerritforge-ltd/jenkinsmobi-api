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
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.NotModifiedNode;
import mobi.jenkinsci.model.ResetNode;
import mobi.jenkinsci.server.core.services.ProxyHttpClientURLDownloader;
import mobi.jenkinsci.server.core.services.RequestCommandDispatcher;

import org.apache.http.HttpStatus;

import com.google.gson.Gson;
import com.google.inject.Inject;

@Slf4j
@Singleton
public class HttpRequestDispatcherServlet extends HttpServlet {
  private static final long serialVersionUID = -1149567900063915796L;

  private final RequestCommandDispatcher pluginRequestDispatcher;

  private final ProxyHttpClientURLDownloader resourcePlugin;

  private final Gson gson;

  private Map<Class<? extends AbstractNode>, Integer> errorStatusMap;

  @Inject
  public HttpRequestDispatcherServlet(
      final RequestCommandDispatcher pluginRequestDispatcher,
      final ProxyHttpClientURLDownloader resourcePlugin, final Gson gson) {
    super();
    this.pluginRequestDispatcher = pluginRequestDispatcher;
    this.resourcePlugin = resourcePlugin;
    this.gson = gson;
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    errorStatusMap = Maps.newHashMap();
    errorStatusMap.put(NotModifiedNode.class, HttpStatus.SC_NOT_MODIFIED);
    errorStatusMap.put(ResetNode.class, HttpStatus.SC_RESET_CONTENT);
  }

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    try {
      final Account account = (Account) req.getUserPrincipal();

      final AbstractNode responseTree =
          pluginRequestDispatcher.getResponse(account, req);
      final String eTag = responseTree.getETag();
      final String reqETag = req.getHeader("If-None-Match");

      final Integer errorResponseStatus = errorStatusMap.get(responseTree.getClass());

      // Error response
      if (errorResponseStatus != null) {
        resp.sendError(errorResponseStatus);
      }

      // Cached response
      else if (reqETag != null && eTag != null && reqETag.equals(eTag)) {
        resp.setStatus(HttpStatus.SC_NOT_MODIFIED);
        log.debug("Resource " + req.getRequestURI()
            + " was NOT MODIFIED: returning HTTP-304");
      }

      // JSON response
      else {
        setResponseHeaders(resp, responseTree);
        resp.setHeader("Content-Type", "application/json");
        responseTree.toStream(resp.getOutputStream());
      }
    } catch (final ResourceNotFoundException e) {
      resp.sendError(HttpStatus.SC_NOT_FOUND);
    } catch (final Exception e) {
      throw new ServletException(e);
    }
  }

  private void setResponseHeaders(final HttpServletResponse resp,
      final AbstractNode responseTree) {
    if (!responseTree.isCacheable()) {
      resp.setHeader("Cache-Control", "no-cache");
    }
    final String eTag = responseTree.getETag();
    if (eTag != null) {
      resp.setHeader("ETag", eTag);
    }
    if (responseTree.getHttpCharacterEncoding() != null) {
      resp.setCharacterEncoding(responseTree.getHttpCharacterEncoding());
    }
  }
}
