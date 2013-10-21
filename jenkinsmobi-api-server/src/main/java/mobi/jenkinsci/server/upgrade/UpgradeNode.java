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
package mobi.jenkinsci.server.upgrade;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.model.RawBinaryNode;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class UpgradeNode extends ItemNode {
  private static final Logger log = Logger.getLogger(UpgradeNode.class);

  @Inject
  private UpgradeConfig upgradeConfig;

  private final UpgradeConfig config;

  public UpgradeNode(final UpgradeConfig config) {
    super(Layout.LIST);
    this.config = config;
    this.icon = "/icons/upgrade.png";
    this.title = "Client Upgrade";
    this.path = "upgrade";
  }

  public ItemNode getUpgradeDetails() {
    if (payload == null || payload.size() <= 0) {
      final ItemNode upgradeAction =
          new ItemNode("Ver. " + config.version + " is available");
      try {
        upgradeAction.setAction(config.url.toLowerCase().startsWith("market")
            ? config.url : "install:?src="
                + URLEncoder.encode(config.url, "utf-8"));
      } catch (final UnsupportedEncodingException e) {
        log.error("Invalid installation URL " + config.url, e);
        return null;
      }

      upgradeAction.setModified("true");
      upgradeAction.setPath("upgrade");
      upgradeAction.setDescription("UPGRADE");
      upgradeAction.setDescriptionAlign(Alignment.RIGHT);
      upgradeAction.setDescriptionColor("#0042A0");
      upgradeAction.setViewTitle("Upgrade");

      addNode(upgradeAction);
      addNode(new HeaderNode("What's new"));
      addNode(new ItemNode(config.description));
      for (final ItemNode releaseNote : config.releaseNotes) {
        addNode(releaseNote);
      }
    }

    return this;
  }

  public UpgradeNode getAvailableUpgrade(final HttpServletRequest request,
      final Account account) throws IOException {
    final UpgradeConfig config = upgradeConfig.find(request, account);
    if(config == null) {
      return null;
    }

    final UpgradeNode upgrade = new UpgradeNode(config);
    return upgrade;
  }

  public boolean isMandatory() {
    return config.mandatory;
  }

  public void doGet(final HttpServletRequest request) {

  }

  public AbstractNode getNode(final HttpServletRequest request)
      throws MalformedURLException, IOException {
    final String src = request.getParameter("src");
    if (src != null) {
      final RawBinaryNode binaryDownload = new RawBinaryNode();
      binaryDownload.setData(new URL(src).openStream());
      return binaryDownload;
    } else {
      return getUpgradeDetails();
    }
  }

}
