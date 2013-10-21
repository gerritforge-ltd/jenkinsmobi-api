package mobi.jenkinsci.server.core.net;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.net.HttpHeaders;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.server.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProxyUtilTest {

  private static final int TEST_PORT = 1810;
  private static final String TEST_HOST_PORT = "http://localhost:" + TEST_PORT;
  private static final String TEST_TARGET_LINK_URL = "http://remoteserver.com:80/remote/context";
  private static final String TEST_REQUEST_LINK_URL = "http://myserver.com/mycontextpath/myrequest";
  private static final String USER_AGENT = "Browser User Agent";
  private static final String PLUGIN_NAME = "MyPlugin";

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(TEST_PORT);
  @Rule
  public WireMockClassRule instanceRule = wireMockRule;

  @Mock
  private Config config;

  private HttpClientFactory httpClientFactory;

  private ProxyUtil proxyUtil;


  @Before
  public void setUp() {
    httpClientFactory = new HttpClientFactory() {
      @Override
      public HttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
      }

      @Override
      public HttpClient getBasicAuthHttpClient(URL url, String user, String password) throws MalformedURLException {
        return HttpClientBuilder.create().build();
      }
    };
    proxyUtil = new ProxyUtil(config, httpClientFactory);
  }

  @Test
  public void proxyfyNonHTMLShouldReturnResourceWithoutModification() throws Exception {
    assertThat(proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME, asStream("My plain body")), is("My plain body"));
  }

  @Test
  public void proxyfyHTMLShouldTransformJavaScriptLinkIntoEmbedded() throws Exception {
    final String jsURI = "/javascript.js";
    final String jsBody = "document.write(\"<p>My First JavaScript</p>\");";
    whenGetThenReturn(jsURI, jsBody);

    final String bodyHeader = "<html><head>";
    final String bodyTrailer = "</head><body></body></html>";

    String proxiedBody = proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME, asStream(bodyHeader +
            "<script language=\"javascript\" type=\"text/javascript\" src=\"" + TEST_HOST_PORT + jsURI +
            "\"></script>" +
            bodyTrailer));

    assertThat(proxiedBody,
            allOf(startsWith(bodyHeader), containsString("<script type=\"text/javascript\">"), containsString(jsBody),
                    containsString("</script>"), endsWith(bodyTrailer)));
  }

  @Test
  public void proxifyHTMLShouldGetLocalResourceForJavaScriptEmbedding() throws Exception {
    final String jsURI = "/javascript.js";
    whenGetThenReturn(jsURI, "wrong remote JS Body");

    final String jsBody = "document.write(\"<p>My First JavaScript</p>\");";
    final File resourceFile = File.createTempFile("javascript", "js");
    writeToFile(jsBody, resourceFile);
    when(config.getFile(Mockito.any(File.class), eq(PLUGIN_NAME), eq(jsURI))).thenReturn(resourceFile);

    assertThat(proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME,
            asStream("<script language=\"javascript\" type=\"text/javascript\" src=\"" + TEST_HOST_PORT + jsURI +
                    "\"></script>")), allOf(containsString(jsBody)));
  }

  private void writeToFile(String jsBody, File resourceFile) throws IOException {
    final FileWriter out = new FileWriter(resourceFile);
    out.write(jsBody);
    out.close();
  }

  private ByteArrayInputStream asStream(String body) throws UnsupportedEncodingException {
    return new ByteArrayInputStream(body.getBytes(ProxyUtil.UTF8_ENCODING));
  }

  @Test
  public void proxyfyHTMLShouldTransformImageLinksIntoEmbeddedAsBase64() throws Exception {
    final String imageURI = "/image.png";
    final byte[] imageData = new byte[]{0, 1, 2, 3, 4};
    whenGetThenReturn(imageURI, "image/png", imageData);

    final String bodyHeader = "<html><body>";
    final String bodyTrailer = "</body></html>";

    String proxiedBody = proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME, asStream(bodyHeader +
            "<img src=\"" + TEST_HOST_PORT + imageURI + "\"/>" +
            bodyTrailer));

    assertThat(proxiedBody, allOf(startsWith(bodyHeader), containsString("<img src=\"data:image/png;base64,"),
            containsString(Base64.encodeBase64String(imageData)), containsString("\"/>"), endsWith(bodyTrailer)));
  }

  @Test
  public void proxyfyHTMLShouldLeaveBase64EncodedImagesUnchanged() throws Exception {
    final String originalBody = "<html><body><img src=\"data:image/png;base64,abcdef\"/></body></html>";

    assertThat(proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME, asStream(originalBody)), is(originalBody));
  }

  @Test
  public void proxyfyHTMLShouldTransformCSSLinksIntoEmbedded() throws Exception {
    final String cssURI = "/style.css";
    final String cssData = "{ background-color:#b0c4de; }";
    whenGetThenReturn(cssURI, cssData);

    final String bodyHeader = "<html><head>";
    final String bodyTrailer = "</head><body></body></html>";

    String proxiedBody = proxyUtil.proxyfy(USER_AGENT, PLUGIN_NAME, asStream(bodyHeader +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + TEST_HOST_PORT + cssURI + "\"/>" +
            bodyTrailer));

    assertThat(proxiedBody, allOf(startsWith(bodyHeader), containsString("<style>"), containsString(cssData),
            containsString("</style>"), endsWith(bodyTrailer)));
  }

  @Test
  public void shouldRewirteRelativeLinksOnHTMLPage() throws Exception {
    final String bodyHeader = "<html><body>";
    final String bodyTrailer = "</body></html>";
    Object linkURI = "/mylink.html";

    String proxiedBody = proxyUtil.rewriteLinks(TEST_TARGET_LINK_URL, TEST_REQUEST_LINK_URL, bodyHeader +
            "<a href=\"" + linkURI + "\"/>" +
            bodyTrailer);

    URL downloadUrl = new URL(TEST_TARGET_LINK_URL);
    final String rewrittenEncodedLink = TEST_REQUEST_LINK_URL + ProxyUtil.WEB_PARAMETER_PREFIX +
            URLEncoder.encode("http://" + downloadUrl.getHost() + ":" + downloadUrl.getPort() + linkURI, "UTF8");
    assertThat(proxiedBody, allOf(startsWith(bodyHeader), containsString("<a href=\"" +
            rewrittenEncodedLink +
            "\"/>"), endsWith(bodyTrailer)));
  }

  private void whenGetThenReturn(String imageURI, String contentType, byte[] imageData) {
    stubFor(get(urlEqualTo(imageURI)).willReturn(
            aResponse().withStatus(HttpStatus.SC_OK).withHeader(HttpHeaders.CONTENT_TYPE, contentType)
                    .withBody(imageData)));
  }

  private void whenGetThenReturn(String jsURI, String jsBody) {
    stubFor(get(urlEqualTo(jsURI)).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(jsBody)));
  }

}
