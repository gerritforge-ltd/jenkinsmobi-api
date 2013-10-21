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
package mobi.jenkinsci.ci.client.sso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;

import mobi.jenkinsci.ci.client.JenkinsFormAuthHttpClient;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class GoogleSsoHandler implements SsoHandler {
  public static final String GOOGLE_ANDROID_APPS_AUTHENTICATOR2_APP_ID =
      "com.google.android.apps.authenticator2";
  static final Logger log = Logger.getLogger(GoogleSsoHandler.class);
  private static final HashMap<FormId, String> ssoFormIds =
      new HashMap<SsoHandler.FormId, String>();

  public static void init() {
    ssoFormIds.put(SsoHandler.FormId.USER, "Email");
    ssoFormIds.put(FormId.PASS, "Passwd");
    JenkinsFormAuthHttpClient.registerSsoHander("google.com",
        new GoogleSsoHandler());
  }

  @Override
  public IOException getException(final HttpResponse response) {
    final StatusLine httpStatusLine = response.getStatusLine();
    final int statusCode = httpStatusLine.getStatusCode();
    switch (statusCode) {
      case HttpURLConnection.HTTP_OK:
        try {
          final Document responseDoc =
              Jsoup.parse(response.getEntity().getContent(), "UTF-8", "");
          final Element errorDiv =
              responseDoc.select("div[id~=(error|errormsg)").first();
          if (errorDiv != null) {
            return new IOException(getDivText(errorDiv));
          }
        } catch (final Exception e) {
        }
        // Break is not needed here: we want to fallback to the 'default' case
        // if no error div is found

      default:
        return new IOException("Google Authentication FAILED");
    }
  }

  @Override
  public String getSsoLoginFieldId(final FormId formId) {
    return ssoFormIds.get(formId);
  }

  @Override
  public String doTwoStepAuthentication(final HttpClient httpClient,
      final HttpContext httpContext, final HttpResponse response,
      final String otp) throws IOException {
    final HttpPost formPost = getOtpFormPost(httpContext, response, otp);
    Element otpResponseForm;
    try {
      final HttpResponse otpResponse =
          httpClient.execute(formPost, httpContext);
      if (otpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        throw getException(otpResponse);
      }

      final Document otpResponseDoc =
          Jsoup.parse(otpResponse.getEntity().getContent(), "UTF-8", "");
      otpResponseForm = otpResponseDoc.select("form[id=hiddenpost]").first();
      if (otpResponseForm == null) {
        final Element errorDiv = otpResponseDoc.select("div[id=error]").first();
        if (errorDiv == null) {
          throw new IOException(
              "2nd-step authentication FAILED: Google did not return positive response form.");
        } else {
          throw new TwoPhaseAuthenticationRequiredException(
              getDivText(errorDiv), GOOGLE_ANDROID_APPS_AUTHENTICATOR2_APP_ID);
        }
      }
    } finally {
      formPost.releaseConnection();
    }

    final HttpPost formCompletePost =
        JenkinsFormAuthHttpClient.getPostForm(
            JenkinsFormAuthHttpClient.getLatestRedirectedUrl(httpContext),
            otpResponseForm, null);
    try {
      final HttpResponse otpCompleteResponse =
          httpClient.execute(formCompletePost, httpContext);
      if (otpCompleteResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
        throw new IOException(
            String
                .format(
                    "2nd-step authentication failed: Google returned HTTP-Status:%d %s",
                    otpCompleteResponse.getStatusLine().getStatusCode(),
                    otpCompleteResponse.getStatusLine().getReasonPhrase()));
      }

      return otpCompleteResponse.getFirstHeader("Location").getValue();
    } finally {
      formCompletePost.releaseConnection();
    }
  }

  private String getDivText(final Element errorDiv) {
    for (final Node child : errorDiv.childNodes()) {
      if (child instanceof TextNode) {
        return ((TextNode) child).getWholeText().trim();
      }
    }
    return "";
  }

  private HttpPost getOtpFormPost(final HttpContext httpContext,
      final HttpResponse response, final String otp) throws IOException,
      MalformedURLException, UnsupportedEncodingException {
    final String requestUri =
        JenkinsFormAuthHttpClient.getLatestRedirectedUrl(httpContext);
    final String requestBaseUrl =
        requestUri.substring(0, requestUri.lastIndexOf('/'));
    log.debug("Looking for HTML input form retrieved from " + requestUri);
    final Document doc =
        Jsoup.parse(response.getEntity().getContent(), "UTF-8", requestBaseUrl);
    final org.jsoup.nodes.Element form =
        doc.select("form[id=verify-form]").first();

    if (otp == null) {
      final Element otpLabel = doc.select("div[id=verifyText]").first();
      throw new TwoPhaseAuthenticationRequiredException(
          "Google 2-step Authenticator: \n"
              + "1. Tap on AuthApp to authenticate.\n" + "2. "
              + getDivText(otpLabel), GOOGLE_ANDROID_APPS_AUTHENTICATOR2_APP_ID);
    }

    final HashMap<String, String> formMapping = new HashMap<String, String>();
    formMapping.put("smsUserPin", otp);
    return JenkinsFormAuthHttpClient.getPostForm(requestBaseUrl, form,
        formMapping);
  }

  @Override
  public String getSsoLoginButtonName() {
    return null;
  }
}
