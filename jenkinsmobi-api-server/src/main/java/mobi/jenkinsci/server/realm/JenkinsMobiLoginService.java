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

import java.io.IOException;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.server.Config;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

@Slf4j
public class JenkinsMobiLoginService extends AbstractLifeCycle implements LoginService {

  public static final String JENKINSMOBI_API_NAME = "JenkinsMobi API";

  private final AccountRegistry registry;

  @Getter
  @Setter
  private IdentityService identityService;

  private final Config config;

  @Inject
  public JenkinsMobiLoginService(AccountRegistry registry, Config config) {
    this.registry = registry;
    this.config = config;
  }

  @Override
  public String getName() {
    return JENKINSMOBI_API_NAME;
  }

  @Override
  public UserIdentity login(String username, Object credentials) {
    try {
      if (!credentials.equals(config.getJenkinsCloudSecret())) {
        log.warn("Invalid password (" + credentials + ")");
        return null;
      }
      Account userAccount = registry.get(username);
      if(userAccount == null) {
        log.warn("Account " + username + " was not found");
        return null;
      }

      return new JenkinsMobiIdentity(userAccount);
    } catch (final IOException e) {
      JenkinsMobiLoginService.log.error("Unexpected exception while trying to authenticate user + " + username, e);
      return null;
    }
  }

  @Override
  public boolean validate(UserIdentity user) {
    return true;
  }

  @Override
  public void logout(UserIdentity user) {
  }
}
