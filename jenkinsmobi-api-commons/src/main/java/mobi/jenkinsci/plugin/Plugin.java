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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;

public interface Plugin {
  AbstractNode processRequest(final Account account,
      final HttpServletRequest request, final PluginConfig pluginConf)
      throws IOException;

  void init();

  void configure(final Properties configuration);

  String getType();

  List<ItemNode> getEntryPoints(final PluginConfig pluginConf)
      throws IOException;

  ItemNode claim(final Account account, final PluginConfig pluginConf,
      final URL url) throws IOException;

  List<ItemNode> getReleaseNotes(final Account account,
      final PluginConfig pluginConf, final String version, final String url,
      final HttpServletRequest request) throws Exception;

  public String validateConfig(final HttpServletRequest req,
      final Account account, final PluginConfig pluginConf)
      throws TwoPhaseAuthenticationRequiredException;

}
