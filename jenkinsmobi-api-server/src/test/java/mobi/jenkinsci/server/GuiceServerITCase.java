package mobi.jenkinsci.server;

import com.jayway.restassured.RestAssured;
import mobi.jenkinsci.server.core.module.PluginsServletContainerModule;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.*;

import static com.jayway.restassured.RestAssured.*;

public class GuiceServerITCase {

  private static final int SERVER_PORT = 18081;

  private GuiceServer guiceServer;

  @Before
  public void setUpClass() throws Exception {
    RestAssured.port = SERVER_PORT;
    guiceServer = new GuiceServer(SERVER_PORT, new PluginsServletContainerModule());
  }

  private void startGuiceServer() throws Exception {
    guiceServer.start();
  }

  @After
  public void tearDown() throws Exception {
    if (guiceServer != null) {
      guiceServer.stop();
    }
  }

  @Test
  public void anonymousRequestShouldRequestWWWBasicAuthenticationHandshake() throws Exception {
    startGuiceServer();

    get("/").then().assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).and()
            .header("WWW-Authenticate", Matchers.containsString("basic realm"));
  }

  @Test
  public void authenticatedRequestWithValidSubscriberIdShouldReturnHTTPOk200() throws Exception {
    System.setProperty(Config.JENKINS_MOBI_HOME_PROPERTY, "./src/test/resources/jenkinsmobi/home");
    startGuiceServer();

    given().auth().basic("username", guiceServer.getInstance(Config.class).getJenkinsCloudSecret()).when().get("/")
            .then().statusCode(HttpStatus.SC_OK);
  }


}
