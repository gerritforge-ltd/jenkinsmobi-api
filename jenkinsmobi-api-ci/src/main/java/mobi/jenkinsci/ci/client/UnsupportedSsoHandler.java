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

import mobi.jenkinsci.ci.client.sso.SsoHandler;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public class UnsupportedSsoHandler implements SsoHandler {
  private final String unsupportedMessage;

  public UnsupportedSsoHandler(final HttpHost host) {
    this.unsupportedMessage =
        "SSO through " + host.getHostName() + " is not supported";
  }

  @Override
  public IOException getException(final HttpResponse response) {
    return new IOException(unsupportedMessage);
  }

  @Override
  public String getSsoLoginFieldId(final FormId formId) {
    throw new IllegalArgumentException(unsupportedMessage);
  }

  @Override
  public String doTwoStepAuthentication(final HttpClient httpClient,
      final HttpContext httpContext, final HttpResponse redirectResponse,
      final String otp) throws IOException {
    throw new IOException(unsupportedMessage);
  }

  @Override
  public String getSsoLoginButtonName() {
    return null;
  }

}
