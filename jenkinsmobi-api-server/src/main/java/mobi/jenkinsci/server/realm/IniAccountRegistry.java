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
package mobi.jenkinsci.server.realm.accounts;

import java.io.IOException;

import com.google.inject.Inject;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Account.Factory;

public class AccountRegistry {

  private final SubscriberStore subscriberStore;

  private final Factory accountFactory;

  @Inject
  public AccountRegistry(final SubscriberStore subscriberStore,
      final Account.Factory accountFactory) {
    super();
    this.subscriberStore = subscriberStore;
    this.accountFactory = accountFactory;
  }

  public Account getAccountBySubscriberId(final String subscriberId)
      throws IOException {
    Account account = subscriberStore.get(subscriberId);
    if (account == null) {
      account = accountFactory.get(subscriberId);
      account.addPlugin(new DefaultJenkinsMobiPlugin(subscriberId));
      subscriberStore.add(account);
    }

    return account;
  }

  public Account saveAccountToSubscribers(final Account account)
      throws IOException {
    return subscriberStore.update(account);
  }
}
