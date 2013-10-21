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
package mobi.jenkinsci.ci.model;

import java.io.IOException;

import mobi.jenkinsci.ci.addon.Utils;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

public class JobName extends JenkinsItem {

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    final ItemNode result = new ItemNode();
    result.setLayout(Layout.LIST);
    result.setTitle(name);
    result.setPath(getNodePathFromFullJenkinsURL(urlPrefix, url));
    result.setVersion(ItemNode.API_VERSION);

    Utils.setIconByLabel(result, color);

    ItemNode currentRootNode = new ItemNode();
    currentRootNode.setPath("build");
    result.addNode(currentRootNode);

    final ItemNode currentNode = new ItemNode();
    Utils.setIconByLabel(currentNode, color);
    currentNode.setAction(path + "?web=" + urlEncode(url));
    currentRootNode.addNode(currentNode);

    currentRootNode = new ItemNode();
    currentRootNode.setPath("details");
    result.addNode(currentRootNode);
    final ItemNode h1 = new HeaderNode("Details");
    currentRootNode.addNode(h1);

    final ItemNode console = new ItemNode("Output console");
    currentRootNode.addNode(console);

    final ItemNode changes = new ItemNode("Recent changes");
    changes.setPath(UrlPath.normalizePath("changes"));
    currentRootNode.addNode(changes);

    result.setLeaf(true);

    return result;
  }

  public boolean isBuilding() {
    return color.contains("_anime");
  }
}
