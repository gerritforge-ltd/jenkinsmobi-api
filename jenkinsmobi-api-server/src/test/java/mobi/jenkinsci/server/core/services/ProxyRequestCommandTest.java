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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Config;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.ProxyUtil;
import mobi.jenkinsci.plugin.PluginConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyRequestCommandTest {
  private static final String A_REQUEST_PATH = "/request";
  private static final String A_REMOTE_RESOURCE_URL =
      "http://somewhere.com/some/remote/resource.html";
  private static final String A_CANONICAL_URL = "http://external.canonical.com";
  private static final String A_REQUEST_CONTEXT_PATH = "/some";
  private static final String A_REQUEST_URL = "http://host.com"
      + A_REQUEST_CONTEXT_PATH + A_REQUEST_PATH;

  @Mock
  private HttpClientURLDownloader mockUrlDownloader;

  @Mock
  private ProxyUtil mockProxyUtil;

  @Mock
  private Config mockConfig;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private Account mockAccount;

  @Mock
  private RawBinaryNode mockResponseNode;

  @InjectMocks
  private ProxyRequestCommand proxyRequestCommand;

  @Before
  public void setUp() throws IOException {
    when(
        mockUrlDownloader.internalQueryForDownload(
            any(HttpServletRequest.class), any(String.class),
            any(Account.class), any(PluginConfig.class))).thenReturn(
        mockResponseNode);
  }

  @Test
  public void shouldAcceptRequestsToProxyRemoteResources() {
    mockRequestWithWebProxyParameter();

    Assert.assertTrue(proxyRequestCommand.canProcess(mockRequest));
  }

  private void mockRequestWithWebProxyParameter() {
    when(mockRequest.getParameter("web")).thenReturn(A_REMOTE_RESOURCE_URL);
    when(mockRequest.getRequestURL()).thenReturn(
        new StringBuffer(A_REQUEST_URL));
    when(mockRequest.getContextPath()).thenReturn(A_REQUEST_CONTEXT_PATH);
  }

  @Test
  public void shouldNotAcceptNonProxyRequests() {
    Assert.assertFalse(proxyRequestCommand.canProcess(mockRequest));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenRequestDoesNotContainRemoteResourceUrl()
      throws IOException {
    proxyRequestCommand.process(mockAccount, mockRequest);
  }

  @Test
  public void whenRequestToProxyRemoteSourceShouldFetchFromRemoteUrl()
      throws IOException {
    mockRequestWithWebProxyParameter();

    proxyRequestCommand.process(mockAccount, mockRequest);

    verify(mockUrlDownloader).internalQueryForDownload(
        any(HttpServletRequest.class), eq(A_REMOTE_RESOURCE_URL),
        any(Account.class), any(PluginConfig.class));
  }

  @Test
  public void whenRequestRemoteHtmlSourceShouldProxyRemoteResourceContent()
      throws IOException {
    mockRequestWithWebProxyParameter();
    when(mockResponseNode.getHttpContentType()).thenReturn("text/html");

    proxyRequestCommand.process(mockAccount, mockRequest);

    verify(mockProxyUtil).proxyfy(any(String.class), any(String.class),
        eq(A_REMOTE_RESOURCE_URL), eq(A_REQUEST_URL), any(String.class),
        any(InputStream.class), any());
  }

  @Test
  public void shouldNotProxyNonHtmlRemoteResources() throws IOException {
    mockRequestWithWebProxyParameter();
    when(mockResponseNode.getHttpContentType()).thenReturn("application/json");

    proxyRequestCommand.process(mockAccount, mockRequest);

    verify(mockProxyUtil, never()).proxyfy(any(String.class),
        any(String.class), any(String.class), any(String.class),
        any(String.class), any(InputStream.class), any());
  }

  @Test
  public void shouldUseCanonicalUrlInProxiedResourceRequests()
      throws IOException {
    mockRequestWithWebProxyParameter();
    when(mockResponseNode.getHttpContentType()).thenReturn("text/html");
    when(mockConfig.getCanonicalUrl()).thenReturn(A_CANONICAL_URL);

    proxyRequestCommand.process(mockAccount, mockRequest);

    verify(mockProxyUtil).proxyfy(any(String.class), any(String.class),
        eq(A_REMOTE_RESOURCE_URL), eq(A_CANONICAL_URL + A_REQUEST_PATH),
        any(String.class), any(InputStream.class), any());

  }
}
