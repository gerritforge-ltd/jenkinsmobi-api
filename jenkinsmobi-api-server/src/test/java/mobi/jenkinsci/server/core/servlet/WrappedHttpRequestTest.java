package mobi.jenkinsci.server.core.servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WrappedHttpRequestTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Test
  public void getOriginalRequestURIWhenBasepathNotFoundInURI() throws Exception {
    String basePath = "/base/path";
    String originalURI = "/mypath/resource.html";
    mockRequestUriWith(originalURI);

    assertThat(new WrappedHttpRequest(mockRequest, basePath, ""),
            hasProperty("requestURI", equalTo(originalURI)));
  }

  private void mockRequestUriWith(String originalURI) {
    when(mockRequest.getRequestURI()).thenReturn(originalURI);
  }

  @Test
  public void getReplacedBasePathWhenBasepathFoundInURI() throws Exception {
    mockRequestUriWith("/base/path/resource.html");

    assertThat(new WrappedHttpRequest(mockRequest,  "/base/path", "/replacement"),
            hasProperty("requestURI", equalTo("/replacement/resource.html")));
  }

  @Test
  public void getOriginalPathInfoWhenBasepathNotFound() throws Exception {
    String originalPathInfo = "/path/info/resource.html";
    mockPathInfoWith(originalPathInfo);

    assertThat(new WrappedHttpRequest(mockRequest, "/other/base/path", ""),
            hasProperty("pathInfo", equalTo(originalPathInfo)));
  }

  @Test
  public void getReplacedPathWhenPathInfoStartsWithBasepathWithReplacementRequested() throws Exception {
    mockPathInfoWith("/other/base/path/resource.html");

    assertThat(new WrappedHttpRequest(mockRequest, "/other/base/path", "/replacement/path"),
            hasProperty("pathInfo", equalTo("/replacement/path/resource.html")));
  }

  private void mockPathInfoWith(String pathInfo) {
    when(mockRequest.getPathInfo()).thenReturn(pathInfo);
  }
}
