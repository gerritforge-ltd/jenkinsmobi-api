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

package mobi.jenkinsci.ci.client;

import mobi.jenkinsci.plugin.PluginConfig;


public class JenkinsConfig {

  public static final int CONSOLE_LOG_MAX_SIZE = 50 * 1024;

  private String name;
  private String filename;
  private String url;
  private String hudsonHostname;
  private String username;
  private String password;
  private boolean useXpath;
  private boolean useHttps;
  private boolean overrideHudsonUrl;
  private boolean useXslt;
  private int connectionTimeout;
  private int portNumber;
  private int maxNumberOfItemsToLoad;
  private int maxConsoleOutputSize;
  private String hudsonVersion;
  private String description;
  private String suffix;
  private char reachable;
  
  public final PluginConfig pluginConfig;

  public JenkinsConfig(PluginConfig pluginConf) {
    this.pluginConfig = pluginConf;
    String url = pluginConf.getUrl();
    String username = pluginConf.getUsername();
    String password = pluginConf.getPassword();
    int port = UrlParser.getPort(url);
    String protocol = UrlParser.getProtocol(url);
    String domainName = UrlParser.getDomainName(url);
    String queryPath = UrlParser.getQueryPath(url); // in this case this act
                                                    // as the suffix part ;)

    if (port == 80 && "http".equals(protocol)) {

      port = 0;
    } else if (port == 443 && "https".equals(protocol)) {

      port = 0;
    }

    if (queryPath != null && queryPath.length() > 0) {
      if (queryPath.startsWith("/")) {

        queryPath = queryPath.substring(1);
      }
      if (queryPath.endsWith("/")) {

        queryPath = queryPath.substring(0, queryPath.length() - 1);
      }
    }

    this.description = domainName;
    this.portNumber = port;
    this.hudsonHostname = domainName;
    this.useHttps = "https".equals(protocol);
    this.suffix = queryPath;
    this.username = username;
    this.password = password;
  }

  public boolean isEqualTo(JenkinsConfig confObject) {

    boolean result = true;

    if (!compareString(confObject.getUrl(), getUrl())) {

      result = false;
    } else if (confObject.isUseXpath() != isUseXpath()) {

      result = false;
    } else if (!compareString(confObject.getSuffix(), getSuffix())) {

      result = false;
    } else if (confObject.isUseXslt() != isUseXslt()) {

      result = false;
    } else if (confObject.isOverrideHudsonUrl() != isOverrideHudsonUrl()) {

      result = false;
    } else if (confObject.getConnectionTimeout() != getConnectionTimeout()) {

      result = false;
    } else if (confObject.getMaxNumberOfItemsToLoad() != getMaxNumberOfItemsToLoad()) {

      result = false;
    } else if (!compareString(confObject.getDescription(), getDescription())) {

      result = false;
    } else if (!compareString(confObject.getUsername(), getUsername())) {

      result = false;
    } else if (!compareString(confObject.getPassword(), getPassword())) {

      result = false;
    } else if (confObject.getMaxConsoleOutputSize() != getMaxConsoleOutputSize()) {

      result = false;
    }

    return result;
  }

  private boolean compareString(String str1, String str2) {

    if (str1 == null && str2 == null) {

      return true;
    }

    if (str1 != null && str2 != null) {

      return str1.equals(str2);
    }

    return false;
  }

  @Override
  public boolean equals(Object antherObject) {

    boolean result = false;

    if (antherObject != null && antherObject instanceof JenkinsConfig) {

      JenkinsConfig __conf = (JenkinsConfig) antherObject;

      if (__conf.getHudsonHostname() == null && this.hudsonHostname == null) {

        result = true;
      } else if (__conf.getHudsonHostname() != null
          && __conf.getHudsonHostname().equalsIgnoreCase(this.hudsonHostname)) {
        result = true;
      }
    }

    return result;
  }

  public int getMaxNumberOfItemsToLoad() {
    return maxNumberOfItemsToLoad;
  }

  public void setMaxNumberOfItemsToLoad(int maxNumberOfItemsToLoad) {
    this.maxNumberOfItemsToLoad = maxNumberOfItemsToLoad;
  }

  public boolean isOverrideHudsonUrl() {
    return overrideHudsonUrl;
  }

  public void setOverrideHudsonUrl(boolean overrideHudsonUrl) {
    this.overrideHudsonUrl = overrideHudsonUrl;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getUrl() {

    if (url == null) {

      String result = null;

      if (suffix == null) {
        suffix = "";
      }

      if (portNumber == 0) {

        if (suffix.length() == 0) {

          result = (useHttps ? "https" : "http") + "://" + hudsonHostname;
        } else {

          result =
              (useHttps ? "https" : "http") + "://" + hudsonHostname + "/"
                  + suffix;
        }
      } else {

        if (suffix.length() == 0) {

          result =
              (useHttps ? "https" : "http") + "://" + hudsonHostname + ":"
                  + portNumber;
        } else {

          result =
              (useHttps ? "https" : "http") + "://" + hudsonHostname + ":"
                  + portNumber + "/" + suffix;
        }
      }

      url = result;
    }

    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHudsonHostname() {
    return hudsonHostname;
  }

  public void setHudsonHostname(String hudsonHostname) {
    this.hudsonHostname = hudsonHostname;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isUseXpath() {
    return useXpath;
  }

  public void setUseXpath(boolean useXpath) {
    this.useXpath = useXpath;
  }

  public boolean isUseXslt() {
    return useXslt;
  }

  public void setUseXslt(boolean useXslt) {
    this.useXslt = useXslt;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public void setPortNumber(int portNumber) {
    this.portNumber = portNumber;
  }

  public String getHudsonVersion() {
    return hudsonVersion;
  }

  public void setHudsonVersion(String hudsonVersion) {
    this.hudsonVersion = hudsonVersion;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isUseHttps() {
    return useHttps;
  }

  public void setUseHttps(boolean useHttps) {
    this.useHttps = useHttps;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public int getMaxConsoleOutputSize() {
    return maxConsoleOutputSize;
  }

  public void setMaxConsoleOutputSize(int maxConsoleOutputSize) {
    this.maxConsoleOutputSize = maxConsoleOutputSize;
  }

  public char getReachable() {
    return reachable;
  }

  public void setReachable(char reachable) {
    this.reachable = reachable;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {

    return description;
  }
}
