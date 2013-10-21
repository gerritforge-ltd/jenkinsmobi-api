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
package mobi.jenkinsci.ci.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mobi.jenkinsci.ci.client.sso.SsoHandler;
import mobi.jenkinsci.ci.client.sso.SsoHandler.FormId;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JenkinsFormAuthHttpClient implements HttpClient {
  private static final Logger log = Logger
      .getLogger(JenkinsFormAuthHttpClient.class);
  private final HttpContext sessionContext;
  private final HttpClient httpClient;

  private static final HashMap<String, SsoHandler> ssoHandlers =
      new HashMap<String, SsoHandler>();
  public static SsoHandler registerSsoHander(final String domain, final SsoHandler handler) {
    return ssoHandlers.put(domain, handler);
  }

  public JenkinsFormAuthHttpClient(final HttpClient httpClient, final String baseUrl,
      final String user, final String password, final String otp) throws IOException {
    this.httpClient = httpClient;
    sessionContext =
        doFormLogin("securityRealm/commenceLogin?from=%2F", baseUrl, user,
            password, otp);
  }

  private HttpContext doFormLogin(final String formLoginUrl, final String baseUrlString,
 final String user, final String password,
      final String otp) throws IOException {
    final URL baseUrl = new URL(baseUrlString);
    final HttpContext httpContext = new BasicHttpContext();
    final CookieStore cookieStore = new BasicCookieStore();
    httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

    final HttpPost postForm =
        getSSOLoginForm(formLoginUrl, baseUrlString, user, password,
            httpContext);
    final String redirectUrl = doSsoLogin(httpContext, postForm);
    doSsoRedirect(baseUrl, httpContext, redirectUrl, otp);

    return httpContext;
  }

  private void doSsoRedirect(final URL baseUrl, final HttpContext httpContext,
      String redirectUrl, final String otp) throws IOException {
    HttpResponse redirectResponse;
    final HttpGet redirect2Jenkins = new HttpGet(redirectUrl);
    log.debug("Login SUCCEDED: redirecting back to Jenkins using "
        + redirect2Jenkins.getURI());
    try {
      redirectResponse = httpClient.execute(redirect2Jenkins, httpContext);
      final HttpHost host =
          (HttpHost) httpContext
              .getAttribute(ExecutionContext.HTTP_TARGET_HOST);

      if (!host.getHostName().toLowerCase()
          .equals(baseUrl.getHost().toLowerCase())) {
        redirectUrl =
            getSsoErrorHandler(host).doTwoStepAuthentication(httpClient,
                httpContext, redirectResponse, otp);
        doSsoRedirect(baseUrl, httpContext, redirectUrl, null);
      }

      if (redirectResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException(
            "Redirection back to Jenkins failed with HTTP Status Code: "
                + redirectResponse.getStatusLine());
      }
    } finally {
      redirect2Jenkins.releaseConnection();
    }
  }



  private String doSsoLogin(final HttpContext httpContext, final HttpPost postForm)
 throws IOException {
    HttpResponse response;
    log.debug("Login via posting form-data to " + postForm.getURI());
    try {
      response = httpClient.execute(postForm, httpContext);
      if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
        throw getSsoExceptionFromFormResults(httpContext, response);
      }
      return response.getFirstHeader("Location").getValue();
    } finally {
      postForm.releaseConnection();
    }
  }

  private HttpPost getSSOLoginForm(final String formLoginUrl, final String baseUrlString,
      final String user, final String password, final HttpContext httpContext)
      throws MalformedURLException, IOException, ClientProtocolException {
    final String loginFormUrl = getUrl(baseUrlString, formLoginUrl);
    log.debug("Fetching login form from " + loginFormUrl);
    final HttpGet getForm = new HttpGet(loginFormUrl);
    getForm.setHeader("Referer", baseUrlString);
    HttpPost postForm = null;
    try {
      final HttpResponse response = httpClient.execute(getForm, httpContext);
      postForm = getForm(httpContext, response, user, password);
    } finally {
      getForm.releaseConnection();
    }
    return postForm;
  }

  private IOException getSsoExceptionFromFormResults(
      final HttpContext httpContext,
      final HttpResponse response) {
    final HttpHost host =
        (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
    final SsoHandler errorHandler = getSsoErrorHandler(host);
    return errorHandler.getException(response);
  }

  private SsoHandler getSsoErrorHandler(final HttpHost host) {
    SsoHandler handler = ssoHandlers.get(getDomain(host));
    if(handler == null) {
      handler = new UnsupportedSsoHandler(host);
    }
    return handler;
  }

  private HttpPost getForm(final HttpContext httpContext, final HttpResponse response,
      final String user, final String password) throws IllegalStateException, IOException {
    final HttpEntity entity = response.getEntity();
    final HttpHost host =
        (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
    final String requestUri = getLatestRedirectedUrl(httpContext);
    final String requestBaseUrl =
        requestUri.substring(0, requestUri.lastIndexOf('/'));
    final String userFormId = getHtmlElementId(host, FormId.USER);
    final String passFormId = getHtmlElementId(host, FormId.PASS);
    final String loginFormId = getHtmlElementId(host, FormId.LOGIN_FORM);
    final String loginButton = getSsoErrorHandler(host).getSsoLoginButtonName();

    log.debug("Looking for HTML input form retrieved from " + requestUri);

    final List<NameValuePair> formNvps = new ArrayList<NameValuePair>();

    final Document doc = Jsoup.parse(entity.getContent(), "UTF-8", requestBaseUrl);
    final org.jsoup.nodes.Element form =
        doc.select(
            "form" + (loginFormId == null ? "" : "[id=" + loginFormId + "]"))
            .first();
    final String formAction = form.attr("action");
    final HttpPost formPost = new HttpPost(getUrl(requestBaseUrl, formAction));
    final Elements formFields = form.select("input");
    for (final Element element : formFields) {
      final String fieldName = element.attr("name");
      String fieldValue = element.attr("value");
      final String fieldId = element.attr("id");

      log.debug(String.format(
          "Processing form field: name='%s' value='%s' id='%s'", fieldName,
          fieldValue, fieldId));

      if (fieldId.equalsIgnoreCase(userFormId)) {
        fieldValue = user;
        log.debug(String.format("Set formField user='%s'", user));
      } else if (fieldId.equalsIgnoreCase(passFormId)) {
        log.debug("Set formField password='*******'");
        fieldValue = password;
      }

      if (loginButton != null
          && element.attr("type").equalsIgnoreCase("submit")) {
        if (element.attr("name").equalsIgnoreCase(loginButton)) {
          formNvps.add(new BasicNameValuePair(fieldName, fieldValue));
        }
      } else {
        formNvps.add(new BasicNameValuePair(fieldName, fieldValue));
      }
    }

    formPost.setEntity(new UrlEncodedFormEntity(formNvps, "UTF-8"));
    return formPost;
  }

  public static String getLatestRedirectedUrl(final HttpContext httpContext) {
    final HttpRequest request =
        (HttpRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
    final HttpHost host =
        (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
    final String requestUri =
        host.getSchemeName() + "://" + host.getHostName()
            + (host.getPort() <= 0 ? "" : ":" + host.getPort())
            + request.getRequestLine().getUri();
    return requestUri;
  }

  private String getHtmlElementId(final HttpHost host, final FormId formId)
      throws IOException {
    return getSsoErrorHandler(host).getSsoLoginFieldId(formId);
  }

  private String getDomain(final HttpHost host) {
    String hostname = host.getHostName().toLowerCase();
    final String[] hostnameParts = hostname.split("\\.");
    if (hostnameParts[hostnameParts.length - 1].equals("uk")) {
      hostname =
          String.format("%s.%s.%s", hostnameParts[hostnameParts.length - 3],
              hostnameParts[hostnameParts.length - 2],
              hostnameParts[hostnameParts.length - 1]);
    } else {
      hostname =
          String.format("%s.%s", hostnameParts[hostnameParts.length - 2],
              hostnameParts[hostnameParts.length - 1]);
    }
    return hostname;
  }

  public static String getUrl(final String baseUrl, final String path)
      throws MalformedURLException {
    if (path.startsWith("http")) {
      return path;
    }

    final URL url = new URL(baseUrl);
    if (path.startsWith("/")) {
      return getDomainFromUrl(url) + path;
    } else {
      return getDomainFromUrl(url) + "/" + url.getPath() + (url.getPath().endsWith("/") ? "":"/") + path;
    }
  }

  public static final String getDomainFromUrl(final URL url) {
    return String.format("%s://%s%s", url.getProtocol(), url.getHost(),
        url.getPort() > 0 ? ":" + url.getPort() : "");
  }

  @Override
  public HttpParams getParams() {
    return httpClient.getParams();
  }

  @Override
  public ClientConnectionManager getConnectionManager() {
    return httpClient.getConnectionManager();
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request) throws IOException,
      ClientProtocolException {
    return httpClient.execute(request, sessionContext);
  }

  @Override
  public HttpResponse execute(final HttpUriRequest request, final HttpContext context)
      throws IOException, ClientProtocolException {
    return (HttpResponse) throwNotSupported();
  }

  private Object throwNotSupported() {
    throw new IllegalArgumentException("Method not supported");
  }

  @Override
  public HttpResponse execute(final HttpHost target, final HttpRequest request)
      throws IOException, ClientProtocolException {
    return httpClient.execute(target, request, sessionContext);
  }

  @Override
  public HttpResponse execute(final HttpHost target, final HttpRequest request,
      final HttpContext context) throws IOException, ClientProtocolException {
    return (HttpResponse) throwNotSupported();
  }

  @Override
  public <T> T execute(final HttpUriRequest request,
      final ResponseHandler<? extends T> responseHandler) throws IOException,
      ClientProtocolException {
    return httpClient.execute(request, responseHandler, sessionContext);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T execute(final HttpUriRequest request,
      final ResponseHandler<? extends T> responseHandler, final HttpContext context)
      throws IOException, ClientProtocolException {
    return (T) throwNotSupported();
  }

  @Override
  public <T> T execute(final HttpHost target, final HttpRequest request,
      final ResponseHandler<? extends T> responseHandler) throws IOException,
      ClientProtocolException {
    return httpClient.execute(target, request, responseHandler, sessionContext);
  }

  @Override
  public <T> T execute(final HttpHost target, final HttpRequest request,
      final ResponseHandler<? extends T> responseHandler, final HttpContext context)
      throws IOException, ClientProtocolException {
    return (T) throwNotSupported();
  }

  public static HttpPost getPostForm(final String requestBaseUrl, final Element form,
      final HashMap<String, String> formMapping) throws MalformedURLException {
    final List<NameValuePair> formNvps = new ArrayList<NameValuePair>();
    final String formAction = form.attr("action");
    final HttpPost formPost =
        new HttpPost(getUrl(requestBaseUrl,
            formAction));
    final Elements formFields = form.select("input");
    for (final Element element : formFields) {
      final String fieldName = element.attr("name");
      String fieldValue = element.attr("value");
      final String fieldId = element.attr("id");

      if (formMapping != null) {
        final String mappedValue = formMapping.get(fieldId);
        if (mappedValue != null) {
          fieldValue = mappedValue;
        }
      }

      log.debug(String.format(
          "Processing form field: name='%s' value='%s' id='%s'", fieldName,
          fieldValue, fieldId));
      formNvps.add(new BasicNameValuePair(fieldName, fieldValue));
    }
    try {
      formPost.setEntity(new UrlEncodedFormEntity(formNvps, "UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      // This would never happen
      throw new IllegalArgumentException("UTF-8 not recognised");
    }

    return formPost;
  }


}
