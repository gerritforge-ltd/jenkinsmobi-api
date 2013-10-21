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

import java.net.MalformedURLException;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

public class JobModules extends JobDetail {

  public JobModules(Job job) {
    super(job);
  }
  
  @Override
  public ItemNode toAbstractNode(String urlPrefix) throws MalformedURLException {
    if (job.modules == null) {
      return null;
    }
    
    ItemNode modules = new ItemNode("Modules");
    modules.setPath(UrlPath.normalizePath("modules"));
    modules.setLayout(Layout.LIST);
    modules.setVersion(ItemNode.API_VERSION);
    modules.setViewTitle(job.name);
    
    // maven modules
    if (job.modules != null) {
      modules.setLayout(Layout.LIST);
      for (MavenMobule hudsonMavenMobule : job.modules) {
        hudsonMavenMobule.path =
            UrlPath.normalizePath(hudsonMavenMobule.displayName);
        modules.addNode(hudsonMavenMobule.toAbstractNode(urlPrefix));
      }
    }
    
    modules.setLeaf(true);
    return modules;
  }

}
