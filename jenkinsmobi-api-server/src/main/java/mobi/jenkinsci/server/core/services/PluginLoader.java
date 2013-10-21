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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginFactory;
import mobi.jenkinsci.plugin.StoredPlugin;
import mobi.jenkinsci.server.Config;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * Each plugin is a classname into a file: that class needs to be in the
 * classpath
 */

public class PluginLoader implements PluginFactory {
  private static final Logger log = Logger.getLogger(PluginLoader.class);

  private Map<String, StoredPlugin> plugins = Maps.newHashMap();
  private Map<String, Plugin> pluginsInstances = Maps.newHashMap();

  private final Config config;

  @Inject
  public PluginLoader(Config config) {
    this.config = config;
  }

  public void loadPlugins() {
    final File f = config.getPluginsHome();
    if (f.isDirectory()) {
      final File[] files = f.listFiles();
      for (final File file : files) {
        if (file.getName().endsWith(".jar")) {
          final StoredPlugin plugin = loadStoredPlugin(file.getAbsolutePath());
          if (plugin != null) {
            plugins.put(plugin.getType(), plugin);
          }
        }
      }
    }
  }

  private StoredPlugin loadStoredPlugin(final String filename) {
    log.info("Loading plugin from " + filename);
    StoredPlugin result = new StoredPlugin();
    try {
      result.setJarFile(filename);
      readImplementationClass(result);
      initPlugin(result);
    } catch (final Exception e) {
      result = null;
      e.printStackTrace();
    }

    return result;
  }

  private void initPlugin(final StoredPlugin result) throws IOException {
    try {
      final Class<Plugin> implementationClass = result.getImplementationClass();
      final Plugin plugin = implementationClass.newInstance();
      pluginsInstances.put(implementationClass.getCanonicalName(), plugin);
      result.setType(plugin.getType());
      plugin.init();
    } catch (final InstantiationException|IllegalAccessException e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void readImplementationClass(final StoredPlugin result) throws IOException, ClassNotFoundException {
    final JarFile jarFile = new JarFile(result.getJarFile());

    try {
      final Manifest mf = jarFile.getManifest();
      final String implementation = mf.getMainAttributes().getValue("impl");
      result.setImplementationClassName(implementation);

      final File file = new File(result.getJarFile());
      final URL url = file.toURI().toURL();
      final URL[] urls = new URL[]{url};
      final ClassLoader newClassLoader = new URLClassLoader(urls, getClass().getClassLoader());
      result.setImplementationClass((Class<Plugin>) newClassLoader.loadClass(result.getImplementationClassName()));

    } finally {
      jarFile.close();
    }
  }

  @Override
  public Plugin get(final String pluginType) {
    final StoredPlugin storedPlugin = plugins.get(pluginType);
    if (storedPlugin == null) {
      log.error("Cannot load plugin " + pluginType);
      return null;
    }

    final Class<Plugin> implementationClass = storedPlugin.getImplementationClass();
    return pluginsInstances.get(implementationClass.getCanonicalName());
  }

}
