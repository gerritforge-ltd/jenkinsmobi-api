package mobi.jenkinsci.server.upgrade;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.server.core.services.PluginLoader;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.jenkinsci.server.Config;

@Slf4j
public class UpgradeNodeFactory {

  private final UpgradeConfig.Factory upgradeConfigFactory;
  private final Config config;
  private final PluginLoader pluginLoader;

  @Inject
  public UpgradeNodeFactory(UpgradeConfig.Factory upgradeConfigFactory, Config config, PluginLoader pluginLoader) {
    this.upgradeConfigFactory = upgradeConfigFactory;
    this.config = config;
    this.pluginLoader = pluginLoader;
  }

  public UpgradeNode getAvailableUpgrade(final HttpServletRequest request,
                                         final Account account) throws IOException {
    final UpgradeConfig config = find(request, account);
    if (config == null) {
      return null;
    } else {
      return new UpgradeNode(config);
    }
  }

  private UpgradeConfig find(final HttpServletRequest request,
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
            return upgradeConfigFactory.get(configProps, configNameParts[0], account,
                    request);
          } catch (final Exception e) {
            log.warn("No upgrade available for User-Agent " + userAgentRegex, e);
            return null;
          }
        }
      }
    }

    return null;
  }

  private Properties getUpgradeProperties() {
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
