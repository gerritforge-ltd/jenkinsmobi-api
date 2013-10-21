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

import java.io.IOException;

import javax.annotation.concurrent.ThreadSafe;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginConfig.Key;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import mobi.jenkinsci.server.Config;

@ThreadSafe
public class TailoredEntryPoint extends ItemNode {
  public static final String DEFAULT_USER = "default";

  enum Attribute {
    ROOT_PATH, ICON_TITLE, ICON_IMAGE
  }

  public interface Factory {
    public TailoredEntryPoint get(@Assisted final Account user,
        @Assisted final PluginConfig.Key pluginKey,
        @Assisted final ItemNode entryPoint);
  }

  private final Ini ini;
  private final Account user;
  private final Key pluginKey;
  private final ItemNode entryPoint;


  @Inject
  public TailoredEntryPoint(final Config config,
      @Assisted final Account user, @Assisted final PluginConfig.Key pluginKey,
      @Assisted final ItemNode entryPoint) throws InvalidFileFormatException,
      IOException {
    this.ini = config.loadIni(Config.ACCESS_CONTROL_CONFIG);
    this.user = user;
    this.pluginKey = pluginKey;
    this.entryPoint = entryPoint;
  }

  @Override
  public String getTitle() {
    return getTailoredAttribute(Attribute.ICON_TITLE, entryPoint.getTitle());
  }

  @Override
  public String getIcon() {
    return UrlPath.normalizePath(pluginKey.getName()) + "$/?image=icons/"
        + getTailoredAttribute(Attribute.ICON_IMAGE, entryPoint.getIcon());
  }

  @Override
  public String getPath() {
    return UrlPath.normalizePath(pluginKey.getName()) + "$"
        + getTailoredAttribute(Attribute.ROOT_PATH, entryPoint.getPath());
  }

  private String getTailoredAttribute(final Attribute attribute,
      final String defaultValue) {
    return firstNotNullString(
        get(attribute, getTailoredAttributes(user.getName(), getPluginName())),
        get(attribute, getTailoredAttributes(user.getName(), getPluginType())),
        get(attribute, getTailoredAttributes(DEFAULT_USER, getPluginName())),
        get(attribute, getTailoredAttributes(DEFAULT_USER, getPluginType())),
        defaultValue);
  }

  private String getPluginName() {
    return pluginKey.getName();
  }

  private String getPluginType() {
    return "(" + pluginKey.getType() + ")";
  }

  private String firstNotNullString(final String... values) {
    for (final String value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String get(final Attribute attribute, final String[] values) {
    return (values != null && attribute.ordinal() < values.length
        ? values[attribute.ordinal()] : null);
  }

  private String[] getTailoredAttributes(final String userName,
      final String pluginName) {
    final String value = ini.get(userName, pluginName + "." + entryPoint);
    if (value == null) {
      return null;
    } else {
      return value.split(",");
    }
  }

  public boolean isVisible() {
    return getTailoredAttributes(user.getName(), getPluginName()) != null
        || getTailoredAttributes(user.getName(), getPluginType()) != null
        || getTailoredAttributes(DEFAULT_USER, getPluginName()) != null
        || getTailoredAttributes(DEFAULT_USER, getPluginType()) != null;
  }
}
