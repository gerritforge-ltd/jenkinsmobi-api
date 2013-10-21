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
package mobi.jenkinsci.server;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Slf4j
public class Config {
  public static final String ACCESS_CONTROL_CONFIG = "access.control.ini";
  public static final String PLUGINS_SUBSCRIBERS_CONFIG = "subscribers.plugins.properties";
  public static final String SUBSCRIBERS_CONFIG = "subscribers.ini";
  public static final String UPGRADE_CONFIG = "upgrade.properties";
  public static final String JENKINS_MOBI_HOME_PROPERTY = "JENKINS_MOBI_HOME";

  private static final String JENKINS_CLOUD_SECRET = "WpoHdtbmnQwMdzhErIWtkZWsgF7tKyOIMcIGhn+n0FHo3Thp/9Dcgg";

  @Getter
  private final int httpMaxConnections;
  @Getter
  private final int httpConnectionTimeoutMsec;
  @Getter
  private final int httpReadTimeoutMsec;
  @Getter
  private final long httpCacheMaxObjectSize;
  @Getter
  private final int httpCacheMaxObjects;
  @Getter
  private final File home;
  @Getter
  private final File pluginsHome;
  @Getter
  private final long cacheTTL;
  @Getter
  private final int cacheRefreshPoolSize;
  @Getter
  private final String canonicalUrl;
  @Getter
  private final String jenkinsCloudSecret = JENKINS_CLOUD_SECRET;

  private final HashMap<String, String> MIME_TYPES = new HashMap<String, String>();

  public Config() {
    home = new File(getEnvString(JENKINS_MOBI_HOME_PROPERTY, "."));
    httpMaxConnections = getEnvInt("HTTP_MAX_CONNECTIONS", 100);
    httpConnectionTimeoutMsec = getEnvInt("HTTP_CONNECTION_TIMEOUT_MSEC", 5000);
    httpReadTimeoutMsec = getEnvInt("HTTP_READ_TIMEOUT_MSEC", 5 * 60000);
    httpCacheMaxObjectSize = getEnvInt("HTTP_CACHE_MAX_OBJECT_SIZE", 256 * 1024);
    httpCacheMaxObjects = getEnvInt("HTTP_CACHE_MAX_OBJECTS", 1024);
    cacheTTL = getEnvInt("CACHE_TTL", 30 * 60 * 1000);
    cacheRefreshPoolSize = getEnvInt("CACHE_REFRESH_POOL_SIZE", 10);
    canonicalUrl = getEnvString("CANONICAL_URL", "");
    pluginsHome = new File(home, "plugins");
    MIME_TYPES.put("png", "image/png");
  }

  public String getMimeType(final String resourceFileName) {
    return MIME_TYPES.get(StringUtils.substringAfterLast(resourceFileName, "."));
  }

  private int getEnvInt(final String variableName, final int defaultValue) {
    return Integer.parseInt(getEnvString(variableName, "" + defaultValue));
  }

  private String getEnvString(final String variableName, final String defaultValue) {
    return Objects.firstNonNull(System.getenv(variableName), Objects
            .firstNonNull(System.getProperty(variableName), defaultValue));
  }

  public File getFile(final String... relativePaths) {
    return getFile(home, relativePaths);
  }

  public File getFile(final File basePath, final String... relativePaths) {
    File outFile = home;
    for (final String path : relativePaths) {
      outFile = new File(outFile, path);
    }
    return outFile;
  }

  public Ini loadIni(final String iniFileName) throws IOException {
    return loadIni(iniFileName, false);
  }

  public Ini loadIni(final String iniFileName, boolean createIfNotExists) throws IOException {
    final Ini ini = new Ini();
    File iniFile = getFile(iniFileName);
    if (createIfNotExists && !iniFile.exists()) {
      Config.log.warn("Creating a new INI file " + iniFileName);
      iniFile.createNewFile();
    }
    ini.load(iniFile);
    return ini;
  }
}
