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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.URLDownloader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.google.inject.Inject;

public class HttpClientURLDownloader implements URLDownloader {

  private final HttpClientFactory httpClientFactory;

  @Inject
  public HttpClientURLDownloader(final HttpClientFactory httpClientFactory) {
    this.httpClientFactory = httpClientFactory;
  }


  @Override
  public RawBinaryNode download(final HttpServletRequest req, final String url, final Account account, final PluginConfig pluginConfig)
      throws IOException {

    final HttpClient client = httpClientFactory.getHttpClient();
    final HttpGet get = new HttpGet(url);
    final HttpResponse response = client.execute(get);

    final StatusLine status = response.getStatusLine();
    switch (status.getStatusCode()) {
      case HttpStatus.SC_OK:
        break;

      case HttpStatus.SC_NOT_FOUND:
        throw new ResourceNotFoundException("HTTP-GET " + url
            + " returned 404 NOT-FOUND");

      default:
        throw new IOException("HTTP-GET " + url + " failed: status code "
            + status);
    }

    final RawBinaryNode result = new RawBinaryNode();
    setResponseEntity(response, result);
    setResponseHttpHeaders(response, result);

    return result;
  }


  private void setResponseEntity(final HttpResponse response,
      final RawBinaryNode result) throws IOException {
    final HttpEntity entity = response.getEntity();
    result.setData(entity == null ? null : entity.getContent());
  }


  private void setResponseHttpHeaders(final HttpResponse response,
      final RawBinaryNode result) {
    for (final Header h : response.getAllHeaders()) {
      if (h.getName().equals(HttpHeaders.CONTENT_TYPE)) {
        result.setHttpContentType(h.getValue());
      } else if (h.getName().equals(HttpHeaders.CONTENT_LENGTH)) {
        result.setSize(Long.parseLong(h.getValue()));
      }
    }
  }

}
