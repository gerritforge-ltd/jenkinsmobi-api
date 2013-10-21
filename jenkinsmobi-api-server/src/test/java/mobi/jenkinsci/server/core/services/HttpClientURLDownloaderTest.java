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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.plugin.PluginConfig;

import mobi.jenkinsci.server.core.net.HttpClientURLDownloader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpClientURLDownloaderTest {

  private static final URI A_REMOTE_RESOURCE_URL = URI
      .create("http://somewhere.com/remote/resource.html");

  private static final String A_CONTENT_TYPE = "text/html";

  private static final long A_CONTENT_LENGTH = 4521L;

  @Mock
  protected HttpClientFactory mockHttpClientFactory;

  @Mock
  protected HttpClient mockHttpClient;

  @Mock
  protected Account mockAccount;

  @Mock
  protected PluginConfig mockPluginConfig;

  @Mock
  protected HttpServletRequest mockPluginRequest;

  @Mock
  protected HttpEntity mockHttpEntity;

  @Mock
  protected InputStream mockInputStream;

  @Captor
  protected ArgumentCaptor<HttpRequestBase> httpUriRequestCaptor;

  @InjectMocks
  protected HttpClientURLDownloader httpClientDownloader;

  @Before
  public void setUp() {
    when(mockHttpClientFactory.getHttpClient()).thenReturn(mockHttpClient);
  }

  @Test
  public void httpGetRemoteUrlReturnsRawBinaryNode() throws IOException {
    when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(
        new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",
            1, 1), HttpStatus.SC_OK, "OK")));

    final RawBinaryNode responseNode =
        httpClientDownloader.download(mockPluginRequest, A_REMOTE_RESOURCE_URL
                .toString(), mockAccount, mockPluginConfig);

    verify(mockHttpClient).execute(httpUriRequestCaptor.capture());
    final HttpRequestBase requestValue = httpUriRequestCaptor.getValue();
    assertThat(requestValue, hasProperty("URI", equalTo(A_REMOTE_RESOURCE_URL)));
    assertThat(responseNode, notNullValue(RawBinaryNode.class));
  }

  @Test
  public void rawBinaryNodeShouldHaveContentTypeAndLengthFromHttpResponseHeaders()
      throws IOException {
    final BasicHttpResponse httpResponse = newHttpResponse(HttpStatus.SC_OK);
    httpResponse.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE,
        A_CONTENT_TYPE));
    httpResponse.addHeader(new BasicHeader(HttpHeaders.CONTENT_LENGTH, ""
        + A_CONTENT_LENGTH));
    when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(
        httpResponse);

    final RawBinaryNode responseNode =
        httpClientDownloader.download(mockPluginRequest, A_REMOTE_RESOURCE_URL
                .toString(), mockAccount, mockPluginConfig);
    assertThat(
        responseNode,
        allOf(hasProperty("httpContentType", is(A_CONTENT_TYPE)),
            hasProperty("size", is(A_CONTENT_LENGTH))));
  }

  @Test
  public void rawBinaryNodeHasInputStreamTakenFromHttpResponseContentEntity()
      throws IllegalStateException, IOException {
    final BasicHttpResponse httpResponse = newHttpResponse(HttpStatus.SC_OK);
    when(mockHttpEntity.getContent()).thenReturn(mockInputStream);
    httpResponse.setEntity(mockHttpEntity);
    when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(
        httpResponse);

    final RawBinaryNode responseNode =
        httpClientDownloader.download(mockPluginRequest, A_REMOTE_RESOURCE_URL
                .toString(), mockAccount, mockPluginConfig);
    assertThat(responseNode,
        hasProperty("downloadedObjectData", sameInstance(mockInputStream)));
  }

  private BasicHttpResponse newHttpResponse(final int httpStatus) {
    final BasicHttpResponse httpResponse =
        new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",
            1, 1), httpStatus, ""));
    return httpResponse;
  }

  @Test(expected = ResourceNotFoundException.class)
  public void httpGetRemoteUrlThrowsResourceNotFoundExceptionWhenHttpStatusIs404()
      throws IOException {
    when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(
        new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",
            1, 1), HttpStatus.SC_NOT_FOUND, "NOT FOUND")));

    httpClientDownloader.download(mockPluginRequest, A_REMOTE_RESOURCE_URL.toString(), mockAccount, mockPluginConfig);
  }

  @Test(expected = IOException.class)
  public void httpGetRemoteUrlThrowsIOExceptionWhenHttpStatusIsAnotherError()
      throws IOException {
    when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(
        new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP",
            1, 1), HttpStatus.SC_INTERNAL_SERVER_ERROR, "INTERNAL ERROR")));

    httpClientDownloader.download(mockPluginRequest, A_REMOTE_RESOURCE_URL.toString(), mockAccount, mockPluginConfig);
  }


}
