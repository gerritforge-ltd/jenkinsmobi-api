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

import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.assistedinject.Assisted;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.server.Config;
import mobi.jenkinsci.server.core.services.*;

import com.google.inject.Inject;

@Slf4j
public class UpgradeConfig {
  public final String version;
  public final String url;
  public final String description;
  public final boolean mandatory;
  public List<ItemNode> releaseNotes;

  private final Config config;

  private final PluginLoader pluginLoader;

  public interface Factory {
    UpgradeConfig get(@Assisted final Properties configProps, @Assisted final String upgradeApp,
                      @Assisted final Account account, @Assisted final HttpServletRequest request);
  }

  @Inject
  private UpgradeConfig(
          final Config config, final PluginLoader pluginLoader,
          @Assisted final Properties configProps, @Assisted final String upgradeApp,
      @Assisted final Account account, @Assisted final HttpServletRequest request) throws Exception {
    this.config = config;
    this.pluginLoader = pluginLoader;
    this.version = configProps.getProperty(upgradeApp + ".version");
    this.url = configProps.getProperty(upgradeApp + ".url");

    this.description = configProps.getProperty(upgradeApp + ".description");
    final String plugin =
        configProps.getProperty(upgradeApp + ".description.plugin");
    if (plugin != null) {
      releaseNotes = getReleaseNotesFromPlugin(account, plugin, request);
    }
    this.mandatory =
        Boolean
            .parseBoolean(configProps.getProperty(upgradeApp + ".mandatory"));
  }

  private List<ItemNode> getReleaseNotesFromPlugin(final Account account,
      final String pluginType, final HttpServletRequest request)
      throws Exception {
    final PluginConfig plugin = account.getPluginConfig(pluginType);
    if (plugin == null) {
      return null;
    }

    final Plugin pluginApi = pluginLoader.get(plugin.getKey().getType());
    return pluginApi.getReleaseNotes(account, plugin, version, url, request);
  }





}
