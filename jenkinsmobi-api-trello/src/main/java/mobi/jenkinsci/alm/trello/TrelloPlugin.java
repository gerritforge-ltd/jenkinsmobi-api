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
package mobi.jenkinsci.alm.trello;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.alm.trello.nodes.SprintsEntry;
import mobi.jenkinsci.alm.trello.nodes.SprintsNode;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TrelloPlugin implements Plugin {
  private Injector injector;

  @Override
  public AbstractNode processRequest(final Account account,
      final HttpServletRequest req, final PluginConfig pluginConf)
      throws IOException {
    AbstractNode outNode =
        injector.getInstance(SprintsNode.Factory.class).create();

    final ItemNode rootNode = new ItemNode();
    rootNode.addNode((ItemNode) outNode);
    outNode = rootNode;

    return outNode;
  }

  @Override
  public void init() {
    injector = Guice.createInjector(new TrelloPluginModule());
  }

  @Override
  public String getType() {
    return "Trello";
  }

  @Override
  public List<ItemNode> getEntryPoints(final PluginConfig pluginConf)
      throws IOException {
    return Collections.singletonList((ItemNode) new SprintsEntry());
  }

  @Override
  public ItemNode claim(final Account account, final PluginConfig pluginConf,
      final URL url) throws IOException {
    return null;
  }

  @Override
  public List<ItemNode> getReleaseNotes(final Account account,
      final PluginConfig pluginConf, final String version, final String url,
      final HttpServletRequest request) throws Exception {
    return null;
  }

  @Override
  public String validateConfig(final HttpServletRequest req,
      final Account account, final PluginConfig pluginConf)
      throws TwoPhaseAuthenticationRequiredException {
    return null;
  }
}
