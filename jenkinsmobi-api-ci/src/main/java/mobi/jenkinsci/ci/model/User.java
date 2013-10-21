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

public class User extends JenkinsItem {

  @SerializedName("user")
  private UserFullName user;
  
  @SerializedName("property")
  private UserProperty property;

  public String getEmailAddress() {
    return property.address;
  }
  
  public String getAbsoluteUrl() {
    return user.absoluteUrl;
  }
  
  public String getFullname() {
    return user.fullname;
  }
  
  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle(getFullname());
    result.setPath(UrlPath.normalizePath(path));
    result.setIcon(UrlPath.normalizePath(path)+"?image=icons/user.png");

    ItemNode child = new ItemNode("Name");
    child.setDescriptionAlign(Alignment.RIGHT);
    child.setDescription(getFullname());
    //TODO
    //result.addNode(child);

    // TODO: email address is not available here we should make a 2 call to
    // Jenkins to retrieve it
    // child = new JenkinsCloudDataNode("Email");
    // child.setDescriptionAlign(Alignment.RIGHT);
    // child.setDescription(property.address);
    // child.setAction("mailto://"+property.address);
    // result.addNode(child);
    
    result.setLeaf(true);

    return result;
  }

  public class UserProperty {

    @SerializedName("address")
    public String address;

    public UserProperty() {
      ;
    }
  }
  
  public class UserFullName{
    
    @SerializedName("fullName")
    private String fullname;
    
    @SerializedName("absoluteUrl")
    private String absoluteUrl;
    
    public String getAbsoluteUrl() {
      return absoluteUrl;
    }
    
    public String getFullname() {
      return fullname;
    }
  }
}
