package mobi.jenkinsci.server.core.servlet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.jayway.restassured.RestAssured;
import lombok.Getter;
import lombok.Setter;
import mobi.jenkinsci.server.core.services.PluginLoader;
import mobi.jenkinsci.test.GuiceServerRule;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PluginLoaderFilterTest {

  private static final int SERVER_PORT = 18080;

  static {
    RestAssured.port = SERVER_PORT;
  }

  @Mock
  private PluginLoader mockPluginLoader;

  @Singleton
  static class MockServlet extends HttpServlet {

    @Getter
    private String lastRequestUri;

    @Getter @Setter
    private String responseBody;

    @Getter @Setter
    private int responseCode;

    @Inject
    public MockServlet() {
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      lastRequestUri = ((HttpServletRequest) req).getRequestURI();
      HttpServletResponse httpResponse = (HttpServletResponse) res;
      httpResponse.setStatus(responseCode);
      httpResponse.getWriter().print(responseBody);
    }
  }

  @Inject
  private MockServlet mockServlet;

  @Rule
  public GuiceServerRule guiceServerRule = new GuiceServerRule(SERVER_PORT, new ServletModule() {

    @Override
    protected void configureServlets() {
      filter("*").through(PluginLoaderFilter.class);
      serve("*").with(MockServlet.class);
    }
  });

  @Test
  public void shouldLoadPluginsAtServerStartup() {
    verify(mockPluginLoader).loadPlugins();
  }

  @Test
  public void shouldPassRequestUriDownstream() {
    String requestUri = "/this/is/some/url";
    get(requestUri);
    assertThat(mockServlet.getLastRequestUri(), equalTo(requestUri));
  }

  @Test
  public void shouldReturnOKResponseCodeUpstream() {
    mockServlet.setResponseCode(HttpStatus.SC_OK);
    get("/").then().assertThat().statusCode(HttpStatus.SC_OK);
  }

  @Test
  public void shouldReturnForbiddenResponseCodeUpstream() {
    mockServlet.setResponseCode(HttpStatus.SC_FORBIDDEN);
    get("/").then().assertThat().statusCode(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  public void shouldReturnBodyUpstream() {
    mockServlet.setResponseCode(HttpStatus.SC_OK);
    mockServlet.setResponseBody("my response body");
    get("/").then().assertThat().body(equalTo("my response body"));
  }
}
