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
package mobi.jenkinsci.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import mobi.jenkinsci.commons.Config;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

/*
 * Each plugin is a classname into a file: that class needs to be in the
 * classpath
 */

public class PluginLoader {
  private static final Logger log = Logger.getLogger(PluginLoader.class);

  protected Map<String, StoredPlugin> plugins =
      new HashMap<String, StoredPlugin>();
  protected Map<String, Plugin> pluginsInstances =
      new HashMap<String, Plugin>();

  @Inject
  private Config config;

  private PluginLoader() {
    ;
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

      extractResources(result);
    } catch (final Exception e) {
      result = null;
      e.printStackTrace();
    }

    return result;
  }

  private void initPlugin(final StoredPlugin result) throws IOException {
    try {
      final Class<Plugin> implementationClass =
          result.getImplementationClass();
      final Plugin plugin = implementationClass.newInstance();
      pluginsInstances.put(implementationClass.getCanonicalName(), plugin);
      result.setType(plugin.getType());
      final File propertiesFile =
          config.getFile(config.getPluginsHome(), result.getType(),
              "plugin.properties");
      Properties props = null;
      if (propertiesFile.exists()) {
        final FileInputStream fin = new FileInputStream(propertiesFile);
        try {
          props = new Properties();
          props.load(fin);
        } finally {
          fin.close();
        }
      }
      plugin.configure(props);
      plugin.init();
    } catch (final InstantiationException e) {
      throw new IOException(e);
    } catch (final IllegalAccessException e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void readImplementationClass(final StoredPlugin result) throws IOException,
      MalformedURLException, ClassNotFoundException {
    final JarFile jarFile = new JarFile(result.getJarFile());

    try {
      final Manifest mf = jarFile.getManifest();
      final String implementation = mf.getMainAttributes().getValue("impl");
      result.setImplementationClassName(implementation);

      final File file = new File(result.getJarFile());
      final URL url = file.toURI().toURL();
      final URL[] urls = new URL[] {url};
      final ClassLoader newClassLoader =
          new URLClassLoader(urls, getClass().getClassLoader());
      result.setImplementationClass((Class<Plugin>) newClassLoader
          .loadClass(result.getImplementationClassName()));

    } finally {
      jarFile.close();
    }
  }

  public void removePlugin(final String name) throws IOException {

    final StoredPlugin plugin = plugins.get(name);
    final File iconDir = config.getFile(plugin.getType());
    delete(iconDir);
    plugins.remove(name);
  }

  private void delete(final File file) throws IOException {

    if (file.isDirectory()) {
      if (file.list().length == 0) {
        file.delete();
      } else {
        final String files[] = file.list();
        for (final String temp : files) {
          final File fileDelete = new File(file, temp);
          delete(fileDelete);
        }
        if (file.list().length == 0) {
          file.delete();
        }
      }

    } else {
      file.delete();
    }
  }

  private void extractResources(final StoredPlugin plugin) throws IOException,
      FileNotFoundException {

    final FileInputStream fis = new FileInputStream(plugin.getJarFile());
    final BufferedInputStream bis = new BufferedInputStream(fis);
    final ZipInputStream zis = new ZipInputStream(bis);
    try {
      ZipEntry ze = null;
      while ((ze = zis.getNextEntry()) != null) {
        if (ze.isDirectory()) {
          continue;
        }

        if (ze.getName().startsWith("icons")) {
          final int size = (int) ze.getSize();
          if (size > 0) {
            final byte[] b = new byte[size];
            int rb = 0;
            int chunk = 0;
            while ((size - rb) > 0) {
              chunk = zis.read(b, rb, size - rb);
              if (chunk == -1) {
                break;
              }
              rb += chunk;
            }

            // create icons directory for this pluginname
            final File iconDir = new File(config.getFile(plugin.getType()), "icons");
            if (!iconDir.exists()) {
              iconDir.mkdirs();
            }

            final FileOutputStream fout =
                new FileOutputStream(config.getFile(plugin.getType(),
                    ze.getName()));
            fout.write(b, 0, rb);
            fout.close();
          }
        }
      }
    } finally {
      zis.close();
    }
  }

  public Plugin getPlugin(final String pluginType) {
    final StoredPlugin storedPlugin = plugins.get(pluginType);
    if (storedPlugin == null) {
      log.error("Cannot load plugin " + pluginType);
      return null;
    }

    final Class<Plugin> implementationClass =
        storedPlugin.getImplementationClass();
    return pluginsInstances.get(implementationClass.getCanonicalName());
  }
}
