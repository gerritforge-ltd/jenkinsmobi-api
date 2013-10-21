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
package mobi.jenkinsci.server.core.services;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.server.upgrade.UpgradeNode;

import com.google.inject.Inject;

public class UpgradeRequestCommand implements RequestCommand {

  private final UpgradeNode upgradeNode;

  @Inject
  public UpgradeRequestCommand(final UpgradeNode upgradeNode) {
    super();
    this.upgradeNode = upgradeNode;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return new UrlPath(request).isUpgradePath();
  }

  @Override
  public AbstractNode process(final Account account,
      final HttpServletRequest request) throws IOException {
    final UpgradeNode availableUpgrade =
        upgradeNode.getAvailableUpgrade(request, account);
    if (availableUpgrade == null) {
      throw new ResourceNotFoundException("No upgrade available");
    }

    return availableUpgrade.getNode(request);
  }
}
