package mobi.jenkinsci.server.core.servlet;

import com.google.inject.servlet.ServletModule;
import com.jayway.restassured.RestAssured;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.NotModifiedNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.server.core.services.ProxyHttpClientURLDownloader;
import mobi.jenkinsci.server.core.services.RequestCommandDispatcher;
import mobi.jenkinsci.test.GuiceServerRule;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static mobi.jenkinsci.model.ItemNode.itemNodeBuilder;

@RunWith(MockitoJUnitRunner.class)
public class HttpRequestDispatcherServletTest {

  private static final int HTTP_PORT = 18080;

  @Mock
  private RequestCommandDispatcher mockPluginRequestDispatcher;

  @Mock
  private ProxyHttpClientURLDownloader mockResourcePlugin;

  @Mock
  private ServletConfig mockConfig;

  @Rule
  public GuiceServerRule serverRule = new GuiceServerRule(HTTP_PORT, new ServletModule() {

    @Override
    protected void configureServlets() {
      serve("/").with(HttpRequestDispatcherServlet.class);
    }
  });

  @BeforeClass
  public static void setUp() throws Exception {
    RestAssured.port = HTTP_PORT;
  }

  @Test
  public void shouldReturnStatusOkWhenGettingRootUrl() throws ServletException, IOException {
    givenResponseNodeForDispatcherMock(new RawBinaryNode().withData(new byte[0]));
    get("/").then().assertThat().statusCode(HttpStatus.SC_OK);
  }

  private void givenResponseNodeForDispatcherMock(AbstractNode responseNode) throws IOException {
    when(mockPluginRequestDispatcher.getResponse(any(Account.class), any(HttpServletRequest.class)))
            .thenReturn(responseNode);
  }

  private void givenThrownExceptionForDispatcherMock(Class<? extends Throwable> error) throws IOException {
    when(mockPluginRequestDispatcher.getResponse(any(Account.class), any(HttpServletRequest.class)))
            .thenThrow(error);
  }

  @Test
  public void requestDispatcherReturnsAJsonFormattedNode() throws ServletException, IOException {
    givenResponseNodeForDispatcherMock(new ItemNode("a test node"));
    get("/").then().assertThat().body("title", is("a test node"));
  }

  @Test
  public void shouldReturn404WhenNodeNotFoundExceptionIsThrown() throws ServletException, IOException {
    givenThrownExceptionForDispatcherMock(ResourceNotFoundException.class);
    get("/").then().assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void shouldReturn500WhenExceptionIsThrown() throws IOException {
    givenThrownExceptionForDispatcherMock(IllegalArgumentException.class);
    get("/").then().assertThat().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturn304WhenResourceHasNotBeenModified() throws IOException {
    givenResponseNodeForDispatcherMock(new NotModifiedNode());
    get("/").then().assertThat().statusCode(HttpStatus.SC_NOT_MODIFIED);
  }

  @Test
  public void shouldReturnETagWhenCacheableResourceETagIsSet() throws IOException {
    givenResponseNodeForDispatcherMock(itemNodeBuilder("").eTag("MyETag").build());
    get("/").then().assertThat().header("ETag", is("MyETag"));
  }

  @Test
  public void shouldReturn304WhenResourceETagIsEqualToIfNoneMatchETag() throws IOException {
    givenResponseNodeForDispatcherMock(itemNodeBuilder("").eTag("MyETag").build());
    given().header("If-None-Match", "MyETag").get("/").then().assertThat().statusCode(HttpStatus.SC_NOT_MODIFIED);
  }

  @Test
  public void responseIsNotCacheableIfResourceIsNotCacheable() throws IOException {
    givenResponseNodeForDispatcherMock(itemNodeBuilder("").cacheable(false).build());
    get("/").then().assertThat().header("Cache-Control", is("no-cache"));
  }
}
