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
package mobi.jenkinsci.alm.assembla.objects;

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

public class AssemblaMilestone extends AssemblaItem {
  public String title;
  public String user_id;
  public String created_by;
  public String space_id;
  public boolean is_completed;
  public String updated_by;
  public String release_level;
  public String release_notes;
  public int planner_type;
  public String pretty_release_level;
  
  @Override
  public AssemblaItem init(AssemblaClient client) {
    super.init(client);
    path = UrlPath.normalizePath(title);
    return this;
  }

  public ItemNode serializeToJenkinsCloudObjects() {
    ItemNode result = new ItemNode();
    result.setTitle(title);
    result.setPath(path);
    result.setLayout(Layout.LIST);
    result.setIcon(result.getPath() + "?image=icons/ico-milestone.png");
    result.setVersion(ItemNode.API_VERSION);
    
    return result;
  }
}
