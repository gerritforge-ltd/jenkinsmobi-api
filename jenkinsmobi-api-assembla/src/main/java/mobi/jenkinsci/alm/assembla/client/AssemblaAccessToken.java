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

public class AssemblaAccessToken {
  public String token_type;
  public String access_token;
  public int expires_in;
  public String refresh_token;

  public void renew(AssemblaAccessToken newToken) {
    this.expires_in = newToken.expires_in;
    this.access_token = newToken.access_token;
  }
}
