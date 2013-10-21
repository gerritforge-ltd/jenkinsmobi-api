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

import java.net.URL;
import java.util.List;

import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

public class ChangeSetItem extends JenkinsItem {
  public String id;
  public String commitId;
  public List<Change> paths;
  public String msg;
  private String comment;
  private FlatUser author;
  public Issue issue;

  public static class Issue {
    public Issue(final URL linkUrl, final String headline, final URL iconUrl) {
      this.linkUrl = linkUrl;
      this.headline = headline;
      this.iconUrl = iconUrl;
    }

    public final URL linkUrl;
    public final URL iconUrl;
    public String headline;
  }

  public String getUniqueId() {
    if (id != null) {
      return id;
    } else if (commitId != null) {
      return commitId;
    } else {
      id = Integer.toString(getCommitMessage().hashCode());
      return id;
    }
  }

  public String getMsg() {
    return msg;
  }

  public List<Change> getPaths() {
    return paths;
  }

  public String getCommitMessage() {
    if (msg != null) {
      return msg;
    } else if (comment != null) {
      return comment;
    } else {
      return "";
    }
  }

  public FlatUser getAuthor() {
    return author;
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode(getCommitMessage());
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);

    result.setPath(getUniqueId());

    if (issue != null) {
      ItemNode ticketDetails = new ItemNode(issue.headline);

      if (issue.iconUrl != null) {
        ticketDetails.setIcon(issue.iconUrl.toString());
      }
      if (issue.linkUrl != null) {
        ticketDetails.setAction(issue.linkUrl.toString());
      }
    }

    result.addNode(new HeaderNode("Code changes"));
    result.addNode(new ItemNode("Author: " + getAuthor().getFullname()));
    result.addNode(new ItemNode(getCommitMessage()));
    result.addNode(new ItemNode("ID: " + getUniqueId()));
    if (paths != null) {
      result.addNode(new HeaderNode("Changes"));
      for (Change change : paths) {
        ItemNode pathObject = new ItemNode(change.file);
        pathObject.setIcon(path + "?image=icons/document_" + change.editType
            + ".png");
        result.addNode(pathObject);
      }
    }

    result.setLeaf(true);

    return result;
  }
}
