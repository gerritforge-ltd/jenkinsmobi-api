package mobi.jenkinsci.server.core.services;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.server.core.net.HttpClientURLDownloader;
import mobi.jenkinsci.server.core.net.ProxyUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProxyHttpClientURLDownloaderTest {

  private static final String TEST_CANONICAL_WEB_URL = "http://mydomain.com/somewhere/";
  private static final String NO_CANONICAL_URL_SPECIFIED = null;
  private static final String TEST_HTML_PAGE_CONTENT = "<html><body>My HTML page</body></html>";


  @Mock
  private ProxyUtil mockProxyUtils;

  @Mock
  private HttpClientURLDownloader mockHttpClientDownloader;

  @Mock
  private HttpServletRequest mockHttpServletRequest;

  @Mock
  private Account mockAccount;

  @Mock
  private PluginConfig mockPluginConfig;

  private ProxyHttpClientURLDownloader proxyDownloader;

  private ProxyHttpClientURLDownloader newProxyHttpClientURLDownloader(String canonicalUrl) {
    return new ProxyHttpClientURLDownloader(mockProxyUtils, mockHttpClientDownloader, canonicalUrl);
  }

  @Before
  public void setUp() throws IOException {
    mockHttpClientDownloaderToReturn(new RawBinaryNode());
    proxyDownloader = newProxyHttpClientURLDownloader(NO_CANONICAL_URL_SPECIFIED);
  }

  private void mockHttpClientDownloaderToReturn(RawBinaryNode returnNode) throws IOException {
    when(mockHttpClientDownloader
            .download(any(HttpServletRequest.class), anyString(), any(Account.class), any(PluginConfig.class)))
            .thenReturn(returnNode);
  }

  @Test
  public void shouldDownloadTargetURLFromHttpClientDownloader() throws IOException {
    String targetDownloadUrl = "http://my.targethost.com/mytarget/download.txt";

    proxyDownloader.download(mockHttpServletRequest, targetDownloadUrl, mockAccount, mockPluginConfig);

    verify(mockHttpClientDownloader)
            .download(same(mockHttpServletRequest), eq(targetDownloadUrl), same(mockAccount), same(mockPluginConfig));
  }

  @Test
  public void shouldDownloadURLFromWebParameterFromHttpClientDownloader() throws IOException {
    String webParameterDownloadUrl = "http://my.targethost.com/mytarget/web-parameter-resource.txt";
    when(mockHttpServletRequest.getParameter("web")).thenReturn(webParameterDownloadUrl);

    proxyDownloader.download(mockHttpServletRequest, "http://anyurl.com", mockAccount, mockPluginConfig);

    verify(mockHttpClientDownloader)
            .download(same(mockHttpServletRequest), eq(webParameterDownloadUrl), same(mockAccount), same(mockPluginConfig));
  }

  @Test
  public void shouldProxyDownloadedContentForHtmlPagesDownloaded() throws IOException {
    mockHttpClientDownloaderToReturn(htmlBinaryContentNode());

    String targetDownloadURLString = "http://anyurl.com";
    proxyDownloader.download(mockHttpServletRequest, targetDownloadURLString, mockAccount, mockPluginConfig);

    verify(mockProxyUtils)
            .proxyfy(anyString(), anyString(), eq(targetDownloadURLString),
                    any(InputStream.class));
  }

  @Test
  public void shouldRewirteLinksOnDownloadedHtmlPages() throws IOException {
    mockHttpClientDownloaderToReturn(htmlBinaryContentNode());

    String targetDownloadURLString = "http://anyurl.com";
    proxyDownloader.download(mockHttpServletRequest, targetDownloadURLString, mockAccount, mockPluginConfig);

    verify(mockProxyUtils)
            .rewriteLinks(eq(targetDownloadURLString), anyString(), anyString());
  }

  private RawBinaryNode htmlBinaryContentNode() {
    RawBinaryNode returnNode = new RawBinaryNode();
    returnNode.setHttpContentType("text/html");
    returnNode.setData(TEST_HTML_PAGE_CONTENT);
    return returnNode;
  }

  @Test
  public void shouldUseCanonicalURLOnRequestURLWhenRewritingLinks() throws IOException {
    mockHttpClientDownloaderToReturn(htmlBinaryContentNode());
    proxyDownloader = newProxyHttpClientURLDownloader(TEST_CANONICAL_WEB_URL);
    String requestHostAndPort = "http://myinternal.host.local:9090";
    String requestContextPath = "/context";
    String resourceURI = "/myresource.html";
    when(mockHttpServletRequest.getRequestURI()).thenReturn(requestContextPath + resourceURI);
    when(mockHttpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestHostAndPort + requestContextPath +
            resourceURI));
    when(mockHttpServletRequest.getContextPath()).thenReturn(requestContextPath);

    proxyDownloader.download(mockHttpServletRequest, "http://somewhere.com", mockAccount, mockPluginConfig);

    verify(mockProxyUtils).rewriteLinks(anyString(), eq(StringUtils
            .substringBeforeLast(TEST_CANONICAL_WEB_URL, "/") + resourceURI), anyString());
  }
}
