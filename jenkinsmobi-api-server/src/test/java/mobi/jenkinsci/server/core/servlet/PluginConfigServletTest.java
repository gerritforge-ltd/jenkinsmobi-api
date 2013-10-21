package mobi.jenkinsci.server.core.servlet;

import com.google.inject.servlet.ServletModule;
import com.jayway.restassured.RestAssured;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.server.core.services.PluginLoader;
import mobi.jenkinsci.server.realm.AccountRegistry;
import mobi.jenkinsci.test.GuiceServerRule;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static com.jayway.restassured.RestAssured.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginConfigServletTest {

  private static int SERVER_PORT = 18082;

  @BeforeClass
  public static void setUp() {
    RestAssured.port = SERVER_PORT;
  }

  @Mock
  private Account mockAccount;

  @Mock
  private Plugin mockPlugin;

  @Mock
  private PluginLoader mockPluginLoader;

  @Mock
  private AccountRegistry mockAccountRegistry;

  @Rule
  public GuiceServerRule guiceServerRule = new GuiceServerRule(SERVER_PORT,
          new ServletModule() {

            @Override
            protected void configureServlets() {
              serve("/*").with(PluginConfigServlet.class);
            }
          });

  @Test
  public void httpGetShouldReturn405() {
    get("/").then().assertThat().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void httpPostShouldReturn404WhenSubscriberIdNotPresentInRequestHeader() throws IOException {
    when(mockAccountRegistry.getAccountBySubscriberId(isNull(String.class))).thenReturn(null);
    post("/").then().assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void httpPostWithSubscriberIdAndEmptyBodyShouldReturn400() throws IOException {
    mockAccountRegistryForReturningASubscriber();
    given().header(PluginConfigServlet.HEADER_SUBSCRIBER_ID, "aSubscriberId").post("/").then().assertThat()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void httpPostWithSubscriberIdAndInvalidPluginConfigBodyShouldReturn400WithTheReason() throws IOException {
    mockAccountRegistryForReturningASubscriber();
    when(mockPluginLoader.get(any(String.class))).thenReturn(mockPlugin);
    when(mockPlugin.validateConfig(any(HttpServletRequest.class), any(Account.class), any(PluginConfig.class)))
            .thenReturn("Invalid " + "config");

    given().header(PluginConfigServlet.HEADER_SUBSCRIBER_ID, "aSuscriberId")
            .body("{ key: { name: \"pluginName\", type: \"pluginType\" } }").post("/").then().assertThat()
            .statusCode(HttpStatus.SC_BAD_REQUEST).body(Matchers.containsString("Invalid config"));
  }

  @Test
  public void shouldReturn200WhenSubscriberIdAndValidPluginConfigIsProvided() throws IOException {
    mockAccountRegistryForReturningASubscriber();
    when(mockPluginLoader.get(any(String.class))).thenReturn(mockPlugin);

    given().header(PluginConfigServlet.HEADER_SUBSCRIBER_ID, "aSuscriberId")
            .body("{ key: { name: \"pluginName\", type: \"pluginType\" } }").post("/").then().assertThat()
            .statusCode(HttpStatus.SC_OK);
  }

  private void mockAccountRegistryForReturningASubscriber() throws IOException {
    when(mockAccountRegistry.getAccountBySubscriberId(notNull(String.class))).thenReturn(mockAccount);
  }
}
