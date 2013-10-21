package mobi.jenkinsci.server.core.services;

import lombok.Cleanup;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.server.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PluginLoaderTest {

  private static final String TEST_PLUGIN_JAR = "/jenkinsmobi/home/plugin.jar";
  private static final String TEST_PLUGIN_TYPE = "Trello";
  @Mock
  private Config config;

  private PluginLoader loader;

  File pluginHomeDir;

  @Before
  public void setUp() throws IOException {
    pluginHomeDir = createTempDirectory();
    copyTestPluginToDirectory(TEST_PLUGIN_JAR, pluginHomeDir);
  }

  private void copyTestPluginToDirectory(String pluginPath, File pluginHomeDir) throws IOException {
    @Cleanup FileOutputStream pluginOut = new FileOutputStream(new File(pluginHomeDir, StringUtils
            .substringAfterLast(pluginPath, "/")));
    @Cleanup InputStream pluginIn = PluginLoaderTest.class.getResourceAsStream(pluginPath);

    IOUtils.copy(pluginIn, pluginOut);
  }

  @After
  public void tearDown() throws IOException {
    removeDirectory(pluginHomeDir);
  }

  private void removeDirectory(File dir) throws IOException {
    FileUtils.cleanDirectory(dir);
  }

  private File createTempDirectory() throws IOException {
    File dir = new File(System.getProperty("java.io.tmpdir"), PluginLoaderTest.class.getSimpleName() + System
            .currentTimeMillis());
    if (!dir.mkdirs()) {
      throw new IOException("Cannot create directory " + dir);
    }
    return dir;
  }

  @Test
  public void pluginsAreNotLoadedWhenLoaderIsJustCreated() {
    loader = new PluginLoader(config);

    assertThat(loader.get(TEST_PLUGIN_TYPE), nullValue(Plugin.class));
  }

  @Test
  public void testPluginShouldBeLoadedAfterLoadPlugins() throws Exception {
    mockConfigToReturnPluginHomeDirectory();
    loader = new PluginLoader(config);

    loader.loadPlugins();

    assertThat(loader.get(TEST_PLUGIN_TYPE), notNullValue(Plugin.class));
  }

  @Test
  public void testPluginShouldBeRetrievedWithTheCorrectType() throws Exception {
    mockConfigToReturnPluginHomeDirectory();
    createAndLoadPluginLoader();

    Plugin plugin = loader.get(TEST_PLUGIN_TYPE);

    assertThat(plugin, hasProperty("type", is(TEST_PLUGIN_TYPE)));
  }

  private void createAndLoadPluginLoader() {
    (loader = new PluginLoader(config)).loadPlugins();
  }

  private void mockConfigToReturnPluginHomeDirectory() {
    when(config.getPluginsHome()).thenReturn(pluginHomeDir);
  }
}
