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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import lombok.Delegate;
import lombok.Getter;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.plugin.URLDownloader;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Inject;

@RunWith(MockitoJUnitRunner.class)
public class DownloadRequestCommandTest {
  private static final String A_DOWNLOAD_URL = "http://www.somewhere.com";
  private static final String A_HTTP_REQUEST_PATH = "/pluginId$/";
  private static final String A_PLUGIN_NAME = "pluginName";
  private static final String A_PLUGIN_TYPE = "pluginType";

  @Mock
  private Plugin mockPlugin;

  @Mock
  private PluginConfig mockPluginConfig;

  @Mock
  private RawBinaryNode mockBinaryNode;

  private final PluginConfig.Key pluginKey = new PluginConfig.Key(
      A_PLUGIN_NAME, A_PLUGIN_TYPE);

  public static class FakePluginURLDownloader implements Plugin, URLDownloader {
    @Getter
    private boolean invoked;

    @Delegate
    private final Plugin plugin;

    @Inject
    public FakePluginURLDownloader(final Plugin plugin) {
      super();
      this.plugin = plugin;
    }

    @Override
    public RawBinaryNode internalQueryForDownload(final HttpServletRequest req,
        final String url, final Account account, final PluginConfig pluginConfig)
        throws IOException {
      this.invoked = true;
      return Mockito.mock(RawBinaryNode.class);
    }
  }

  @InjectMocks
  private FakePluginURLDownloader fakePluginURLDownloader;

  @Mock
  private PluginLoader mockPluginLoader;

  @Mock
  private URLDownloader mockUrlDownloader;

  @Mock
  private HttpEntity mockHttpEntity;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private Account mockAccount;

  @InjectMocks
  private DownloadRequestCommand downloadRequestConsumer;

  @Before
  public void setUp() throws ClientProtocolException, IOException {
    when(mockRequest.getPathInfo()).thenReturn(A_HTTP_REQUEST_PATH);
    when(mockAccount.getPluginConfig(any(String.class))).thenReturn(
        mockPluginConfig);
    when(mockPluginConfig.getKey()).thenReturn(pluginKey);
    when(
        mockUrlDownloader.internalQueryForDownload(
            any(HttpServletRequest.class), any(String.class),
            any(Account.class), any(PluginConfig.class))).thenReturn(
        mockBinaryNode);
  }

  @Test
  public void shouldConsuleRequestForDownloadingRemoteURLs() {
    mockDownloadRequest();

    assertTrue(downloadRequestConsumer.canProcess(mockRequest));
  }

  @Test
  public void shouldNotConsumeRequestForOtherRequestsWithoutDownloadParameter() {
    assertFalse(downloadRequestConsumer.canProcess(mockRequest));
  }

  @Test
  public void shouldProcessDownloadThroughPluginProvidedUrlDownloader()
      throws IOException {
    when(mockPluginLoader.getPlugin(A_PLUGIN_TYPE)).thenReturn(
        fakePluginURLDownloader);

    downloadRequestConsumer.process(mockAccount, mockRequest);

    assertTrue(fakePluginURLDownloader.isInvoked());
  }

  @Test
  public void shouldProcessDownloadThroughEmbeddedUrlDownloaderIfPluginDoesNotSupportUrlDownload()
      throws IOException {
    mockDownloadRequest();
    when(mockPluginLoader.getPlugin(A_PLUGIN_TYPE)).thenReturn(mockPlugin);
    downloadRequestConsumer =
        new DownloadRequestCommand(mockPluginLoader, mockUrlDownloader);

    downloadRequestConsumer.process(mockAccount, mockRequest);

    verify(mockUrlDownloader).internalQueryForDownload(
        any(HttpServletRequest.class), any(String.class), same(mockAccount),
        same(mockPluginConfig));
  }

  private void mockDownloadRequest() {
    when(mockRequest.getParameter("download")).thenReturn(A_DOWNLOAD_URL);
  }

  @Test
  public void shouldProcessDownloadThroughEmbeddedUrlDownloaderWhenNoPluginsReturned()
      throws IOException {
    mockDownloadRequest();
    downloadRequestConsumer =
        new DownloadRequestCommand(mockPluginLoader, mockUrlDownloader);

    downloadRequestConsumer.process(mockAccount, mockRequest);

    verify(mockUrlDownloader).internalQueryForDownload(
        any(HttpServletRequest.class), any(String.class), same(mockAccount),
        same(mockPluginConfig));
  }
}
