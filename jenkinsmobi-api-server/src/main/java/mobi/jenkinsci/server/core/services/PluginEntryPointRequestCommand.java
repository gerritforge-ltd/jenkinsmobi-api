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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.server.core.services.TailoredEntryPoint.Factory;
import mobi.jenkinsci.server.upgrade.UpgradeNode;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Slf4j
public class PluginEntryPointRequestCommand implements RequestCommand {

  private final UpgradeNode upgradeNode;

  private final PluginLoader pluginLoader;

  private final TailoredEntryPoint.Factory tailorPluginFactory;

  @Inject
  public PluginEntryPointRequestCommand(final UpgradeNode upgradeNode,
      final PluginLoader pluginLoader, final Factory tailorPluginFactory) {
    super();
    this.upgradeNode = upgradeNode;
    this.pluginLoader = pluginLoader;
    this.tailorPluginFactory = tailorPluginFactory;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return new UrlPath(request).isRootPath();
  }

  @Override
  public AbstractNode process(final Account account,
      final HttpServletRequest request) throws IOException {
    final ItemNode root = new ItemNode();
    root.setLayout(Layout.ICONS);
    root.setPath("/");
    root.setVersion(ItemNode.API_VERSION);

    root.addNode(getUpgradeNode(account, request));
    if (isMandatoryUpgrade(account, request)) {
      return root;
    }

    for (final PluginConfig pluginConf : account.listPluginsConfigs()) {
      log.debug("Loading plugin " + pluginConf.getKey());
      final Plugin pluginInstance =
          pluginLoader.getPlugin(pluginConf.getKey().getType());

      root.addNodes(getEntryPoints(account, pluginConf, pluginInstance));
    }

    return root;
  }

  private UpgradeNode getUpgradeNode(final Account account,
      final HttpServletRequest request) throws IOException {
    return upgradeNode.getAvailableUpgrade(request, account);
  }

  private boolean isMandatoryUpgrade(final Account account,
      final HttpServletRequest request) throws IOException {
    final UpgradeNode upgrade = getUpgradeNode(account, request);
    return upgrade != null && upgrade.isMandatory();
  }

  private List<ItemNode> getEntryPoints(final Account account,
      final PluginConfig pluginConf, final Plugin pluginInstance)
      throws IOException {
    final List<ItemNode> entryPoints = Lists.newArrayList();

    for (final ItemNode entryPoint : pluginInstance.getEntryPoints(pluginConf)) {
      log.debug("Scanning entryPoint " + entryPoint.getPath());

      final TailoredEntryPoint tailoredEntyPoint =
          tailorPluginFactory.get(account, pluginConf.getKey(), entryPoint);
      if (tailoredEntyPoint.isVisible()) {
        entryPoints.add(tailoredEntyPoint);
      }
    }

    return entryPoints;
  }

}
