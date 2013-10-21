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
import java.util.HashMap;

import mobi.jenkinsci.ci.client.JenkinsFormAuthHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public class GitHubSsoHandler implements SsoHandler {

  private static final HashMap<FormId, String> ssoFormIds =
      new HashMap<FormId, String>();

  public static void init() {
    ssoFormIds.put(FormId.USER, "login_field");
    ssoFormIds.put(FormId.PASS, "password");
    JenkinsFormAuthHttpClient.registerSsoHander("github.com",
        new GitHubSsoHandler());
  }

  @Override
  public IOException getException(final HttpResponse response) {
    return new IOException("GitHub Login Failed");
  }

  @Override
  public String getSsoLoginFieldId(final FormId formId) {
    return ssoFormIds.get(formId);
  }

  @Override
  public String doTwoStepAuthentication(final HttpClient httpClient,
      final HttpContext httpContext, final HttpResponse redirectResponse, final String otp)
      throws IOException {
    throw new IOException("Unsupported 2-phase authentication for GitHub SSO");
  }

  @Override
  public String getSsoLoginButtonName() {
    return null;
  }
}
