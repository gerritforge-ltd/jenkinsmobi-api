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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Config;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.google.inject.Inject;

public class UpgradeConfig {
  private static final Logger log = Logger.getLogger(UpgradeConfig.class);

  public final String version;
  public final String url;
  public final String description;
  public final boolean mandatory;
  public List<ItemNode> releaseNotes;

  @Inject
  private Config config;

  @Inject
  private PluginLoader pluginLoader;

  private UpgradeConfig(final Properties configProps, final String upgradeApp,
      final Account account, final HttpServletRequest request) throws Exception {
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

    final Plugin pluginApi = pluginLoader.getPlugin(plugin.getKey().getType());
    return pluginApi.getReleaseNotes(account, plugin, version, url, request);
  }


  public UpgradeConfig find(final HttpServletRequest request,
      final Account account) {
    if (!config.getFile(Config.UPGRADE_CONFIG).exists()) {
      return null;
    }

    final String userAgent = request.getHeader("User-Agent");
    final Properties configProps = getUpgradeProperties();

    final Enumeration<?> configNames = configProps.propertyNames();
    while (configNames.hasMoreElements()) {
      final String configName = ((String) configNames.nextElement());
      final String[] configNameParts = configName.split("\\.");
      if (configNameParts.length != 2) {
        log.debug("Ignoring unknown property " + configName);
      }
      if (configNameParts[1].equalsIgnoreCase("match")) {
        final String userAgentRegex = configProps.getProperty(configName);
        log.debug("Checking upgrade config " + configName + " regex '"
            + userAgentRegex + "' against User-Agent:" + userAgent);
        final Matcher matcher =
            Pattern.compile(userAgentRegex).matcher(userAgent);
        if (matcher.matches()) {
          try {
            return new UpgradeConfig(configProps, configNameParts[0], account,
                request);
          } catch (final Exception e) {
            Log.warn("No upgrade available for User-Agent " + userAgentRegex, e);
            return null;
          }
        }
      }
    }

    return null;
  }

  public Properties getUpgradeProperties() {
    final Properties configProps = new Properties();
    FileReader configReader = null;
    final File upgradeConfigFile = config.getFile(Config.UPGRADE_CONFIG);
    try {
      configReader = new FileReader(upgradeConfigFile);
    } catch (final FileNotFoundException e1) {
      return null;
    }

    try {
      configProps.load(new FileReader(upgradeConfigFile));
    } catch (final IOException e) {
      log.error("Invalid upgrade configuration on " + upgradeConfigFile);
      return null;
    } finally {
      try {
        configReader.close();
      } catch (final IOException e) {
      }
    }

    return configProps;
  }
}
