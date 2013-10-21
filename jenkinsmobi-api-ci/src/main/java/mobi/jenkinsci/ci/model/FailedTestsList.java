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

public class FailedTestsList extends JenkinsItem {

  @SerializedName("suites")
  private List<FailedSuite> suites;
  
  public List<FailedSuite> getSuites() {
    return suites;
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setPath(UrlPath.normalizePath(path));
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle("Failed Tests");
    
    for (FailedSuite suite : suites) {
      suite.path = UrlPath.normalizePath(suite.name);
      ItemNode suiteNode = new ItemNode();
      suiteNode.setLayout(Layout.LIST);
      suiteNode.setTitle(suite.name);
      suiteNode.setPath(suite.path);
      suiteNode.addNode(suite.toAbstractNode(urlPrefix));
      result.addNode(suiteNode);
    }
    
    result.setLeaf(true);

    return result;
  }
}
