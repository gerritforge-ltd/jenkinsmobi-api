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


public class HttpCredentials {

  private String username;
  private String password;

  public HttpCredentials() {

  }

  public HttpCredentials(JenkinsConfig config, String username,
      String password) {
    if (username == null) {
      this.username = config.getUsername();
    } else {
      this.username = username;

    }
    if (password == null) {
      this.password = config.getPassword();
    } else {
      this.password = password;
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
