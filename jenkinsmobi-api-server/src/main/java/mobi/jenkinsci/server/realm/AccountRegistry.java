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
import java.util.Set;

import mobi.jenkinsci.commons.Account;

public interface AccountRegistry {

  Account getAccountBySubscriberId(final String subscriberId)
      throws IOException;

  Account get(final String key) throws IOException;

  void add(final Account obj) throws IOException;

  void update(final Account account) throws IOException;

  Set<String> getRoles(final String name) throws IOException;

}
