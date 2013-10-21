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
package mobi.jenkinsci.alm.assembla.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import mobi.jenkinsci.alm.assembla.objects.AssemblaMilestone;
import mobi.jenkinsci.alm.assembla.objects.AssemblaMilestones;
import mobi.jenkinsci.alm.assembla.objects.AssemblaSpace;
import mobi.jenkinsci.alm.assembla.objects.AssemblaSpaces;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTicket;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTicketComment;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTickets;
import mobi.jenkinsci.net.HttpClientFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;

public class AssemblaClient {
  private static Logger LOG = Logger.getLogger(AssemblaClient.class);
  private static URL ASSEMBLA_SITE;
  static {
    try {
      ASSEMBLA_SITE = new URL("https://api.assembla.com");
    } catch (final MalformedURLException e) {
      // This cannot happen as URL string is hardcoded
      LOG.fatal("Cannot get Assembla URL", e);
    }
  }
  private static String ASSEMBLA_SITE_APP_AUTH =
      "https://%s:%s@api.assembla.com";
  private static final String MY_SPACES = "/v1/spaces.json";
  private static final String SPACE_TICKETS = "/v1/spaces/%s/tickets.json";
  private static final String SPACE_TICKET_COMMENTS =
      "/v1/spaces/%s/tickets/%d/ticket_comments.json";
  private static final String SPACE_MILESTONES =
      "/v1/spaces/%s/milestones.json";
  private static final String AUTH =
      "/authorization?client_id=%s&response_type=pin_code";
  private static final String PIN_AUTH =
      "/token?grant_type=pin_code&pin_code=%s";
  private static final String TOKEN_REFRESH =
      "/token?grant_type=refresh_token&refresh_token=%s";
  private static final String LOGIN = "/login";

  private final String appId;
  private final String appSecret;
  private final String username;
  private final String password;
  private AssemblaAccessToken accessToken;

  @Inject
  private HttpClientFactory httpClientFactory;

  private final HttpClient httpClient;
  private final HttpContext httpContext;
  private static Gson gson;
  static {
    final GsonBuilder gsonB = new GsonBuilder();
    gsonB
        .registerTypeAdapter(GregorianCalendar.class, new CalendarSerializer());
    gson = gsonB.create();
  }

  public AssemblaClient(final String appId, final String appSecret,
      final String username, final String password) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.httpClient = httpClientFactory.getHttpClient();
    httpContext = new BasicHttpContext();
    final CookieStore cookieStore = new BasicCookieStore();
    httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

    this.username = username;
    this.password = password;
  }



  private URL getLatestRedirectedUrl() {
    try {
      final HttpRequest request =
          (HttpRequest) httpContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
      final HttpHost host =
          (HttpHost) httpContext.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
      if (host.getPort() <= 0) {
        return new URL(host.getSchemeName(), host.getHostName(), request
            .getRequestLine().getUri());
      } else {
        return new URL(host.getSchemeName(), host.getHostName(),
            host.getPort(), request.getRequestLine().getUri());
      }
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          "Cannot get last redirected URL from HTTP Context", e);
    }
  }


  public void login() throws IOException {
    Document pinDoc = Jsoup.parse(getData(String.format(AUTH, appId), false));
    if (getLatestRedirectedUrl().getPath().startsWith(LOGIN)) {
      pinDoc = postLoginForm(pinDoc);
    }

    final Element pinBox = pinDoc.select("div[class=box]").first();
    if (pinBox == null) {
      throw new IOException("Missing PIN code from Assembla auth response");
    }
    final Element pinLabel = pinBox.select("p").first();
    final Element pinValue = pinBox.select("h1").first();
    if (pinLabel == null || pinValue == null) {
      throw new IOException("Missing PIN code from Assembla auth response");
    }
    final String pin = pinValue.childNode(0).toString();
    final HttpPost authPost =
        new HttpPost(String.format(ASSEMBLA_SITE_APP_AUTH, appId, appSecret)
            + String.format(PIN_AUTH, pin));
    final HttpResponse pinResponse = httpClient.execute(authPost);
    try {
      if (pinResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Post " + authPost.getURI()
            + " for a PIN failed: " + pinResponse.getStatusLine());
      }
      accessToken =
          gson.fromJson(new JsonReader(new InputStreamReader(pinResponse
              .getEntity().getContent(), "UTF-8")), AssemblaAccessToken.class);
    } finally {
      authPost.releaseConnection();
    }
  }

  public void loginRefresh() throws IOException {
    if (accessToken == null) {
      login();
    } else {
      accessToken.access_token = "";
      final HttpPost authPost =
          new HttpPost(String.format(ASSEMBLA_SITE_APP_AUTH, appId, appSecret)
              + String.format(TOKEN_REFRESH, accessToken.refresh_token));
      final HttpResponse response = httpClient.execute(authPost);
      try {
        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
          throw new IOException("Post " + authPost.getURI()
              + " for Token refresh failed: " + response.getStatusLine());
        }
        final AssemblaAccessToken refreshToken =
            gson.fromJson(new JsonReader(new InputStreamReader(response
                .getEntity().getContent(), "UTF-8")), AssemblaAccessToken.class);
        accessToken.renew(refreshToken);
      } finally {
        authPost.releaseConnection();
      }
    }
  }

  private Document postLoginForm(final Document pinDoc) throws IOException {
    final List<NameValuePair> formNvps = new ArrayList<NameValuePair>();
    final Element form = pinDoc.select("form[id=login-box]").first();
    final String formAction = form.attr("action");
    final HttpPost formPost = new HttpPost(getUrl(formAction).toString());
    final Elements formFields = form.select("input");
    for (final Element element : formFields) {
      final String fieldName = element.attr("name");
      String fieldValue = element.attr("value");
      final String fieldId = element.attr("id");
      final String fieldType = element.attr("type");

      if (fieldId.equalsIgnoreCase("user_login")) {
        fieldValue = username;;
      } else if (fieldId.equalsIgnoreCase("user_password")) {
        fieldValue = password;
      }

      if (fieldType.equals("submit")) {
        if (!fieldName.equalsIgnoreCase("commit")) {
          continue;
        }
      }

      LOG.debug(String.format(
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

    HttpResponse response;
    LOG.debug("Login via posting form-data to " + formPost.getURI());
    try {
      response = sendHttpPost(formPost);
      if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
        throw new IOException("Form-based login to Assembla failed: "
            + response.getStatusLine());
      }
      return Jsoup.parse(getData(
          response.getFirstHeader("Location").getValue(), false));
    } finally {
      formPost.releaseConnection();
    }
  }



  private HttpResponse sendHttpPost(final HttpPost formPost) throws IOException {
    HttpResponse response;
    response = httpClient.execute(formPost, httpContext);
    return response;
  }



  private URL getUrl(final String formAction) {
    try {
      return new URL(ASSEMBLA_SITE, formAction);
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          "Cannot create URL from formAction target " + formAction, e);
    }
  }

  public AssemblaSpaces spaces() throws IOException {
    final List<AssemblaSpace> spaces =
        gson.fromJson(getData(MY_SPACES, true),
            new TypeToken<List<AssemblaSpace>>() {}.getType());

    return (AssemblaSpaces) new AssemblaSpaces(spaces).init(this);
  }

  public AssemblaTickets getTickets(final String spaceId) throws IOException {
    final List<AssemblaTicket> tickets =
        gson.fromJson(getData(String.format(SPACE_TICKETS, spaceId), true),
            new TypeToken<List<AssemblaTicket>>() {}.getType());
    return (AssemblaTickets) new AssemblaTickets(tickets).init(this);
  }


  public AssemblaMilestones getMilestones(final String spaceId)
      throws Exception {
    final List<AssemblaMilestone> milestones =
        gson.fromJson(getData(String.format(SPACE_MILESTONES, spaceId), true),
            new TypeToken<List<AssemblaMilestone>>() {}.getType());
    return (AssemblaMilestones) new AssemblaMilestones(milestones).init(this);
  }

  private String getData(final String dataSuffix, final boolean autoLogin)
      throws IOException {
    loginWhenNecessary(autoLogin);
    final HttpGet get = new HttpGet(getTargetUrl(dataSuffix));
    setRequestHeaders(get, autoLogin);

    try {
      final HttpResponse response = httpClient.execute(get, httpContext);
      switch (response.getStatusLine().getStatusCode()) {
        case HttpURLConnection.HTTP_OK:
          final ByteArrayOutputStream bout = new ByteArrayOutputStream();
          final InputStream in = response.getEntity().getContent();
          IOUtils.copy(in, bout);
          return new String(bout.toByteArray());

        case HttpURLConnection.HTTP_NO_CONTENT:
          return null;

        case HttpURLConnection.HTTP_NOT_FOUND:
          LOG.warn("Resource at " + dataSuffix
              + " was not found: returning NULL");
          return null;

        case HttpURLConnection.HTTP_UNAUTHORIZED:
          if (autoLogin) {
            loginRefresh();
            return getData(dataSuffix, false);
          }

        default:
          throw new IOException("Cannot GET " + dataSuffix + " from Assembla: "
              + response.getStatusLine());
      }
    } finally {
      get.releaseConnection();
    }
  }

  private String postData(final String dataSuffix, final byte[] postData,
      final ContentType contentType) throws IOException {
    loginWhenNecessary(true);
    final HttpPost post = new HttpPost(getTargetUrl(dataSuffix));
    setRequestHeaders(post, true);
    post.setEntity(new ByteArrayEntity(postData, contentType));

    try {
      final HttpResponse response = httpClient.execute(post, httpContext);
      switch (response.getStatusLine().getStatusCode()) {
        case HttpURLConnection.HTTP_CREATED:
          final ByteArrayOutputStream bout = new ByteArrayOutputStream();
          final InputStream in = response.getEntity().getContent();
          IOUtils.copy(in, bout);
          return new String(bout.toByteArray());

        default:
          throw new IOException("Cannot POST " + dataSuffix + " to Assembla: "
              + response.getStatusLine());
      }
    } finally {
      post.releaseConnection();
    }
  }

  private String getTargetUrl(final String dataSuffix) {
    final String targetUrl =
        (dataSuffix.startsWith("http") ? dataSuffix : getUrl(dataSuffix)
            .toString());
    return targetUrl;
  }

  private void setRequestHeaders(final HttpRequestBase req,
      final boolean autoLogin) {
    if (accessToken != null) {
      req.addHeader("Authorization", "Bearer " + accessToken.access_token);
    }
    if (req.getURI().getPath().endsWith(".json")) {
      req.addHeader(HttpHeaders.ACCEPT, "application/json");
    }
  }



  private void loginWhenNecessary(final boolean autoLogin) throws IOException {
    if (accessToken == null && autoLogin) {
      login();
    }
  }

  public AssemblaTicketComment addComment(final String spaceId,
      final int ticketNumber, final String commentText) throws IOException {
    final JsonObject comment = new JsonObject();
    comment.add("comment", new JsonPrimitive(commentText));
    final JsonObject ticketComment = new JsonObject();
    ticketComment.add("ticket_comment", comment);

    final AssemblaTicketComment assemblaComment =
        gson.fromJson(
            postData(
                String.format(SPACE_TICKET_COMMENTS, spaceId, ticketNumber),
                ticketComment.toString().getBytes(),
                ContentType.APPLICATION_JSON), AssemblaTicketComment.class);
    return (AssemblaTicketComment) assemblaComment.init(this);
  }
}
