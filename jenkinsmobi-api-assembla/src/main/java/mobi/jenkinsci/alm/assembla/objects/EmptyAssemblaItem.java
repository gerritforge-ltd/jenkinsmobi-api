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

import mobi.jenkinsci.model.EmptyNode;
import mobi.jenkinsci.model.ItemNode;

public class EmptyAssemblaItem extends AssemblaItem {
  private String path;

  public EmptyAssemblaItem(String path) {
    this.path = path;
  }
  
  @Override
  public ItemNode serializeToJenkinsCloudObjects() {
    return new EmptyNode(path);
  }

  @Override
  public boolean hasPath(String path) {
    return path.equalsIgnoreCase(path);
  }
}
