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

import java.util.List;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.google.gson.annotations.SerializedName;

public class ComputerList extends JenkinsItem {

  @SerializedName("computer")
  private List<Computer> computerList;

  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setPath(UrlPath.normalizePath(path));
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle("Computers");
    result.setIcon(result.getPath()+"?image=icons/nodes_wall.png");

    for (Computer computer : computerList) {
      computer.path = UrlPath.normalizePath(computer.displayName);
      result.addNode(computer.toAbstractNode(urlPrefix));
    }
    
    result.setLeaf(true);

    return result;
  }
}
