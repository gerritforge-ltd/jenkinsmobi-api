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

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.google.gson.annotations.SerializedName;

public class FailedTest extends JenkinsItem {

  @SerializedName("failedSince")
  private String failedSince;
  @SerializedName("status")
  private String status;
  
  public String getFailedSince() {
    return failedSince;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setPath(UrlPath.normalizePath(path));
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle(name);
    
    ItemNode n = new ItemNode();
    n.setTitle("Test");
    n.setDescription(name);
    n.setDescriptionAlign(Alignment.RIGHT);
    result.addNode(n);
    
    n = new ItemNode();
    n.setTitle("Failed Since");
    n.setDescription(failedSince);
    n.setDescriptionAlign(Alignment.RIGHT);
    result.addNode(n);
    
    n = new ItemNode();
    n.setTitle("Status");
    n.setDescription(status);
    n.setDescriptionAlign(Alignment.RIGHT);
    if(status.equalsIgnoreCase("FAILED")){
     n.setDescriptionColor("#FF0000"); 
    }else if(status.equalsIgnoreCase("SUCCESS")){
      n.setDescriptionColor("#00FF00"); 
    }
    result.addNode(n);
    result.setLeaf(true);
    
    return result;
  }
}
