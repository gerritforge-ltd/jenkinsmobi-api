package mobi.jenkinsci.server.core.net;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import mobi.jenkinsci.server.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

public class PooledHttpClientFactoryTest {

  public static final int TEST_PORT = 1809;
  private static final String TEST_USERNAME = "myUsername";
  private static final String TEST_PASSWORD = "myPassword123";
  private final String LOCALHOST_PORT = "http://localhost:" + TEST_PORT;

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(TEST_PORT);

  @Rule
  public WireMockClassRule instanceRule = wireMockRule;

  @Test
  public void getHttpClientReturnsAllowsAnonymousGet() throws Exception {
    final String resourceUri = "/my/resource";
    final int httpStatus = HttpStatus.SC_OK;
    mockHttpToReturn(resourceUri, httpStatus);

    PooledHttpClientFactory factory = new PooledHttpClientFactory(new Config());
    HttpClient http = factory.getHttpClient();

    HttpResponse response = http.execute(new HttpGet(LOCALHOST_PORT + resourceUri));
    assertThat(response, hasProperty("statusLine", hasProperty("statusCode", is(httpStatus))));
  }

  private void mockHttpToReturn(String resourceUri, int httpStatus) {
    stubFor(get(urlEqualTo(resourceUri))
            .willReturn(aResponse().withStatus(httpStatus).withHeader("Content-Type", "text/plain")
                    .withBody("something")));
  }

  private void mockBasciAuthHttpToReturn(String resourceUri, String username, String password, int httpStatus) {
    stubFor(get(urlEqualTo(resourceUri)).willReturn(aResponse().withHeader("WWW-Authenticate", "Basic realm=\"TEST\"")
            .withStatus(HttpStatus.SC_UNAUTHORIZED)));

    final String base64Auth = new String(Base64.encodeBase64((username + ":" + password).getBytes()));
    stubFor(get(urlEqualTo(resourceUri)).withHeader("Authorization", equalTo("Basic " + base64Auth))
            .willReturn(aResponse().withStatus(httpStatus).withHeader("Content-Type", "text/plain")
                    .withBody("something secure")));

  }

  @Test
  public void getHttpClientIsForbiddenToGetAuthenticatedResources() throws Exception {
    final String resourceUri = "/my/resource/secure";
    mockBasciAuthHttpToReturn(resourceUri, TEST_USERNAME, TEST_PASSWORD, HttpStatus.SC_OK);

    PooledHttpClientFactory factory = new PooledHttpClientFactory(new Config());
    HttpClient http = factory.getHttpClient();

    HttpResponse response = http.execute(new HttpGet(LOCALHOST_PORT + resourceUri));
    assertThat(response, hasProperty("statusLine", hasProperty("statusCode", is(HttpStatus.SC_UNAUTHORIZED))));
  }

  @Test
  public void getBasicAuthHttpClientReturnsOkToGetAuthenticatedResources() throws Exception {
    final String resourceUri = "/my/resource/secure";
    final String targetURLString = LOCALHOST_PORT + resourceUri;
    mockBasciAuthHttpToReturn(resourceUri, TEST_USERNAME, TEST_PASSWORD, HttpStatus.SC_OK);

    PooledHttpClientFactory factory = new PooledHttpClientFactory(new Config());
    HttpClient http = factory
            .getBasicAuthHttpClient(new URL(targetURLString), TEST_USERNAME, TEST_PASSWORD);

    HttpResponse response = http.execute(new HttpGet(targetURLString));
    assertThat(response, hasProperty("statusLine", hasProperty("statusCode", is(HttpStatus.SC_OK))));
  }
}
