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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Account.Factory;
import mobi.jenkinsci.plugin.PluginConfig;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import mobi.jenkinsci.server.Config;

@Singleton
public class IniAccountRegistry implements AccountRegistry {

  private final Account.Factory accountFactory;
  private final Config config;
  private final Ini accountsIni;

  @Inject
  public IniAccountRegistry(final Config config, final Factory accountFactory) throws IOException {
    this.config = config;
    this.accountFactory = accountFactory;
    this.accountsIni = config.loadIni(Config.SUBSCRIBERS_CONFIG, true);
  }

  @Override
  public Account get(final String name) throws IOException {
    final Section accountSection = accountsIni.get(name);
    if (accountSection == null) {
      return null;
    }

    final Set<String> roles = Sets.newHashSet(accountSection.get("roles", "").split(","));
    final Account account = accountFactory.get(name, roles);
    account.addPlugins(getPlugins(accountSection.getChild("plugins")));
    return account;
  }

  private Collection<PluginConfig> getPlugins(final Section pluginsSection) throws IOException {
    final List<PluginConfig> pluginsConfigs = Lists.newArrayList();

    if (pluginsSection != null) {
      for (final String pluginName : pluginsSection.childrenNames()) {
        final Section pluginSection = pluginsSection.getChild(pluginName);
        final PluginConfig pluginConfig = new PluginConfig(pluginName, pluginSection.get("type"));
        pluginConfig.setDescription(pluginSection.get("description", ""));
        pluginConfig.setUrl(pluginSection.get("url", ""));
        pluginConfig.setUsername(pluginSection.get("username", ""));
        pluginConfig.setPassword(pluginSection.get("password", ""));
        pluginConfig.setOptions(getPluginOptions(pluginSection.getChild("options")));
        pluginsConfigs.add(pluginConfig);
      }
    }

    return pluginsConfigs;
  }

  private Map<String, String> getPluginOptions(final Section optionsSection) {
    final Map<String, String> options = Maps.newHashMap();
    if (optionsSection != null) {
      for (final String key : optionsSection.keySet()) {
        options.put(key, optionsSection.get(key));
      }
    }
    return options;
  }


  @Override
  public void add(final Account account) throws IOException {
    final Section accountSection = accountsIni.add(account.getName());
    accountSection.add("roles", Joiner.on(",").join(account.getRoles()));
    addPluginsToSection(account.getPluginsConfigs(), accountSection.addChild("plugins"));
    accountsIni.store(config.getFile(Config.SUBSCRIBERS_CONFIG));
  }

  private void addPluginsToSection(final Collection<PluginConfig> plugins, final Section pluginsSection) {
    for (final PluginConfig plugin : plugins) {
      final Section pluginSection = pluginsSection.addChild(plugin.getKey().getName());

      pluginSection.add("type", plugin.getKey().getType());
      pluginSection.add("description", plugin.getDescription());
      pluginSection.add("url", plugin.getUrl());
      pluginSection.add("username", plugin.getUsername());
      pluginSection.add("password", plugin.getPassword());

      setPluginOptionsToSection(pluginSection.addChild("options"), plugin.getOptions());
    }
  }

  private void setPluginOptionsToSection(final Section optionsSection, final Map<String, String> options) {
    optionsSection.clear();
    if (options == null) {
      return;
    }

    for (final Entry<String, String> option : options.entrySet()) {
      optionsSection.put(option.getKey(), option.getValue());
    }
  }

  @Override
  public synchronized Account getAccountBySubscriberId(final String subscriberId) throws IOException {
    Account account = get(subscriberId);
    if (account == null) {
      account = createDefaultAccount(subscriberId);
    }

    return account;
  }

  private Account createDefaultAccount(final String subscriberId) throws IOException {
    final Account account = accountFactory.get(subscriberId, new HashSet<String>());
    account.addPlugin(new DefaultJenkinsMobiPlugin(subscriberId));
    add(account);
    return account;
  }

  @Override
  public void update(final Account account) throws IOException {
    accountsIni.remove(account.getName());
    add(account);
  }
}
