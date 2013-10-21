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
package mobi.jenkinsci.exceptions;

import java.io.IOException;

public class TwoPhaseAuthenticationRequiredException extends IOException {
  private static final long serialVersionUID = 6390763647784727072L;
  private String authAppId;

  public String getAuthAppId() {
    return authAppId;
  }

  public void setAuthAppId(final String authAppId) {
    this.authAppId = authAppId;
  }

  public TwoPhaseAuthenticationRequiredException(final String string, final String authAppId) {
    super(string);
    setAuthAppId(authAppId);
  }

  public TwoPhaseAuthenticationRequiredException(final String string) {
    super(string);
  }
}
