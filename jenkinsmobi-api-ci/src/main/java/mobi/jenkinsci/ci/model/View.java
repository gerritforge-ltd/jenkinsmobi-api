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
import java.util.LinkedList;
import java.util.List;

import mobi.jenkinsci.ci.client.JenkinsClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;

public class View extends JenkinsItem {

  @SerializedName("jobs")
  private List<JobName> jobs;

  public List<JobName> getJobNames() {

    if (jobs == null) {
      jobs = new LinkedList<JobName>();
    }

    return jobs;
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    final ItemNode result = new ItemNode();
    result.setTitle(name);
    result.setPath(getNodePathFromFullJenkinsURL(urlPrefix, url));
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setDescription("");

    for (final JobName job : jobs) {
      job.path = JenkinsClient.urlEncode(job.name);
      result.addNode(job.toAbstractNode(urlPrefix));
    }

    addMenu(result);
    result.setLeaf(true);

    return result;
  }

  private void addMenu(final ItemNode result) {

    ItemNode menuEntry = new ItemNode();
    menuEntry.setTitle("All");
    menuEntry.setAction("select://icon/*");
    result.addMenuNode(menuEntry);

    menuEntry = new ItemNode();
    menuEntry.setTitle("Stable");
    menuEntry.setAction("select://icon/stable.png");
    menuEntry.setIcon(result.getPath()+"?image=icons/stable.png");
    result.addMenuNode(menuEntry);

    menuEntry = new ItemNode();
    menuEntry.setTitle("Unstable");
    menuEntry.setAction("select://icon/unstable.png");
    menuEntry.setIcon(result.getPath()+"?image=icons/unstable.png");
    result.addMenuNode(menuEntry);

    menuEntry = new ItemNode();
    menuEntry.setTitle("Failed");
    menuEntry.setIcon(result.getPath()+"?image=icons/alert.png");
    menuEntry.setAction("select://icon/alert.png");
    result.addMenuNode(menuEntry);

    menuEntry = new ItemNode();
    menuEntry.setTitle("Disabled/Aborted");
    menuEntry.setIcon(result.getPath()+"?image=icons/disabled.png");
    menuEntry.setAction("select://icon/disabled.png");
    result.addMenuNode(menuEntry);
  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    final JenkinsItem subItem = super.getSubItem(plugin, subItemPath);

    if(Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }
    final Job job = client().getJob(getHeadPath(subItemPath));
    return job.getSubItem(plugin, getTailPath(subItemPath));
  }
}
