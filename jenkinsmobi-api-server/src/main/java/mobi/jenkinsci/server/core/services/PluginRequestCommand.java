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
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.server.core.servlet.WrappedHttpRequest;

public class PluginRequestCommand implements RequestCommand {

  private final PluginLoader pluginLoader;

  public PluginRequestCommand(final PluginLoader pluginLoader) {
    super();
    this.pluginLoader = pluginLoader;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return null != new UrlPath(request).getPluginId();
  }

  @Override
  public AbstractNode process(final Account account,
      final HttpServletRequest request) throws IOException {
    final UrlPath path = new UrlPath(request);
    final PluginConfig config = account.getPluginConfig(path.getPluginId());
    final Plugin plugin = pluginLoader.getPlugin(config.getKey().getType());

    return getResponseLevelAtDepth(
        dispatchToPlugin(account, new WrappedHttpRequest(request), path,
            plugin, config), path);
  }

  private AbstractNode getResponseLevelAtDepth(final AbstractNode response,
      final UrlPath path) throws ResourceNotFoundException {
    if (path.getDepth() != UrlPath.INFINITE_DEPTH
        && !response.isLeaf() && response instanceof ItemNode) {
      return path.cutTreeToDepth((ItemNode) response);
    } else {
      return response;
    }
  }

  private AbstractNode dispatchToPlugin(final Account account,
      final HttpServletRequest request, final UrlPath urlPath,
      final Plugin pluginInstance, final PluginConfig pluginConf)
      throws IOException {
    AbstractNode response =
        pluginInstance.processRequest(account, request, pluginConf);

    if (response == null) {
      throw new ResourceNotFoundException("Plugin " + pluginInstance
          + " did not produce any response");
    }

    if ("application/json".equals(response.getHttpContentType())
        && response instanceof ItemNode) {
      response = urlPath.followPath((ItemNode) response);
    }
    return response;
  }

}
