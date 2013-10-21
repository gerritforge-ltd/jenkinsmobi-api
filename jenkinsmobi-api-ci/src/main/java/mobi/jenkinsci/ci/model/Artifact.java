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
import mobi.jenkinsci.net.UrlPath;

import com.google.gson.annotations.SerializedName;

public class Artifact extends JenkinsItem {

  @SerializedName("displayPath")
  private String displayPath;

  @SerializedName("relativePath")
  private String relativePath;

  public String getDisplayPath() {
    return displayPath;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public void setDisplayPath(String displayPath) {
    this.displayPath = displayPath;
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {
    boolean isAndroidPackage = displayPath.endsWith(".apk");

    ItemNode result = new ItemNode();
    result.setPath(UrlPath.normalizePath(path));
    result.setTitle(displayPath);
    result.setIcon(displayPath+"?image=icons/" + (isAndroidPackage ? "android.png":"artifact.png"));
    result.setDescription("INSTALL");
    result.setDescriptionAlign(Alignment.RIGHT);
    String action = displayPath+"?download=" + urlEncode(path);
    if(isAndroidPackage) {
      action = "install:" + action;
    }
    result.setAction(action);
    result.setLeaf(true);
    return result;
  }
}
