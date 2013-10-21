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
package mobi.jenkinsci.commons;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import lombok.Getter;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

@ThreadSafe
public class Account implements Principal {

  @Getter
  public final String name;

  private final ConcurrentMap<String, PluginConfig> plugins;

  private final PluginLoader pluginLoader;

  @Getter
  private final Set<String> roles;

  public interface Factory {
    public Account get(@Assisted final String name,
        @Assisted final Set<String> roles);
  }

  @Inject
  public Account(@Assisted final String name,
      @Assisted final Set<String> roles, final PluginLoader pluginLoader) {
    this.name = name;
    this.pluginLoader = pluginLoader;
    this.plugins = new ConcurrentHashMap<String, PluginConfig>();
    this.roles = roles;
  }

  public Collection<PluginConfig> listPluginsConfigs() {
    return plugins.values();
  }

  public PluginConfig getPluginConfig(final String pluginName) {
    return plugins.get(pluginName);
  }

  public void addPlugin(final PluginConfig updatePlugin) {
    plugins.put(updatePlugin.getKey().getName(), updatePlugin);
  }

  public ItemNode getPluginNodeForUrl(final PluginConfig pluginConf,
      final URL url) throws IOException {
    ItemNode node = null;
    for (final PluginConfig jenkinsCloudPlugin : plugins.values()) {
      final Plugin pluginInstance =
          pluginLoader.getPlugin(jenkinsCloudPlugin.getKey().getType());
      node = pluginInstance.claim(this, jenkinsCloudPlugin, url);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "Account [subscriberId=" + name + ", plugins=" + plugins + "]";
  }

  public Collection<PluginConfig> getPluginsConfigs() {
    return plugins.values();
  }

  public void addPlugins(final Collection<PluginConfig> pluginsConfigs) {
    for (final PluginConfig pluginConfig : pluginsConfigs) {
      addPlugin(pluginConfig);
    }
  }
}
