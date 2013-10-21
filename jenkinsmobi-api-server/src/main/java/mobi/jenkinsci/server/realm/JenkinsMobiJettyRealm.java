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
package mobi.jenkinsci.server.realm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.security.HashUserRealm;

@Slf4j
public class JenkinsMobiJettyRealm extends HashUserRealm {

  private AccountRegistry registry;

  // FIXME: this hardcoded key needs to change to a configurable appId/secret
  // pair
  private static final String JENKINS_COULD_USERNAME =
      "hnkbo5lbqrlWLNiUSkKWK8RwWmczpMUU9USAWygWaI5AeNiBHK9Or+";
  private static final String JENKINS_CLOUD_SECRET =
      "WpoHdtbmnQwMdzhErIWtkZWsgF7tKyOIMcIGhn+n0FHo3Thp/9Dcgg";

  @Override
  public Principal authenticate(final String username,
      final Object credentials, final Request request) {
    try {
      if (!username.equals(JENKINS_COULD_USERNAME)
          || !credentials.equals(JENKINS_CLOUD_SECRET)) {
        log.warn("Invalid username (" + username + ") or password + ("
            + credentials + ")");
        return null;
      }
      return registry.get(getPayload(request));
    } catch (final IOException e) {
      log.error("Unexpected exception while trying to authenticate user + "
          + username, e);
      return null;
    }
  }

  private String getPayload(final HttpServletRequest req) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(req.getInputStream(), out);
    out.close();
    return new String(out.toByteArray(), req.getCharacterEncoding());
  }

  @Override
  public synchronized boolean isUserInRole(final Principal user,
      final String roleName) {
    try {
      return registry.getRoles(user.getName()).contains(roleName);
    } catch (final IOException e) {
      log.error("Unexpected exception while getting roles for  user " + user
          + " from registry", e);
      return false;
    }
  }

  @Override
  public String getName() {
    return "JenkinsMobi API";
  }
}
