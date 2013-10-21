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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobi.jenkinsci.ci.addon.Utils;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;

public class Build extends JenkinsItem {

  public int number;
  public int duration;
  public long timestamp;
  public List<Artifact> artifacts;
  public ChangeSet changeSet;
  public List<HudsonAction> actions;
  public String result;
  public boolean building;

  private Job job;

  public void setJob(final Job job) {
    this.job = job;
  }

  private final SimpleDateFormat dateFmt = new SimpleDateFormat(
      "EEE, d MMM yyyy HH:mm");

  public long builtSince() {

    final long now = new Date().getTime();
    return now - timestamp;
  }

  public String durationeHumanReadable() {
    return humanReadableElapsed(duration / 1000);
  }

  public String builtSinceHumanReadable() {
    final long now = new Date().getTime();
    final long seconds = (now - timestamp) / 1000;

    if (seconds < 0) {
      return "few seconds";
    }
    return humanReadableElapsed(seconds);
  }

  private String humanReadableElapsed(long seconds) {

    if (seconds > 60) {

      long minutes = seconds / 60;

      if (minutes > 60) {

        long hours = minutes / 60;

        if (hours > 24) {

          long days = hours / 24;

          if (days > 30) {

            final long months = days / 30;
            days = days % 30;

            return "" + months + " months and " + days + " days";

          } else {

            hours = hours % 24;
            return "" + days + " days and " + hours + " hours";
          }

        } else {

          minutes = minutes % 60;
          return "" + hours + " hours and " + minutes + " minutes";
        }
      } else {

        seconds = seconds % 60;
        return "" + minutes + " minutes and " + seconds + " seconds";
      }
    } else {

      return "" + seconds + " seconds";
    }
  }

  public class HudsonAction {
    @SerializedName("failCount")
    private String failCount;
    @SerializedName("skipCount")
    private String skipCount;
    @SerializedName("totalCount")
    private String totalCount;

    public String getFailCount() {
      return failCount;
    }

    public String getSkipCount() {
      return skipCount;
    }

    public String getTotalCount() {
      return totalCount;
    }
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    final ItemNode node = new ItemNode("Build #" + number);
    node.setViewTitle("Build #" + number + " of " + job.name);
    node.setLayout(Layout.LIST);
    node.setDescription("(" + dateFmt.format(new Date(timestamp)) + ")");
    node.setDescriptionAlign(Alignment.BOTTOM);
    Utils.setIconByLabel(node, result);

    if (job != null) {
      node.addNodes(getBuildSubNodes(job, urlPrefix, lazyLoad));
    }
    node.setLeaf(true);
    return node;
  }

  public List<ItemNode> getBuildSubNodes(final Job job, final String urlPrefix,
      final boolean lazyLoad) throws IOException {
    final ArrayList<ItemNode> subNodes = new ArrayList<ItemNode>();

    final JobInstallPackage installPackage =
        new JobInstallPackage(job, this, lazyLoad);
    if (installPackage.isAndroidCompatible()) {
      subNodes.add(installPackage.toAbstractNode(urlPrefix));
    }

    if (actions != null) {
      for (final HudsonAction a : actions) {
        if (a.getFailCount() != null && Integer.parseInt(a.getFailCount()) > 0) {
          final ItemNode failedTests = new ItemNode("Failed Tests");
          failedTests.setDescription(a.getFailCount() + " failed over "
              + a.getTotalCount());
          failedTests.setDescriptionAlign(Alignment.BOTTOM);
          failedTests.setPath(UrlPath.normalizePath("failedtests"));
          failedTests.setLayout(Layout.LIST);
          failedTests.setVersion(ItemNode.API_VERSION);
          failedTests.setAction("?cmd=load://" + url + "testReport");
          subNodes.add(failedTests);
          break;
        }
      }
    }

    final ItemNode console = new ItemNode("Build console");
    console.setPath("console");
    console.setIcon("?image=icons/terminal.png");
    console.setAction("?web=" + urlEncode(url + "/logText/progressiveHtml")
        + "&decorate=html");
    subNodes.add(console);

    subNodes.add(getBuildDetail(Job.Detail.CHANGES, lazyLoad).toAbstractNode(
        urlPrefix));
    subNodes.add(getBuildDetail(Job.Detail.ARTIFACTS, lazyLoad).toAbstractNode(
        urlPrefix));
    subNodes.add(getBuildDetail(Job.Detail.FAILEDTESTS, lazyLoad)
        .toAbstractNode(urlPrefix));
    return subNodes;
  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    JenkinsItem subItem = super.getSubItem(plugin, subItemPath);

    if (Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }
    final Job.Detail detail =
        Job.Detail.valueOf(getHeadPath(subItemPath).toUpperCase());
    subItem = getBuildDetail(detail, false);

    if (subItem != null) {
      return subItem.getSubItem(plugin, getTailPath(subItemPath));
    } else {
      return null;
    }
  }

  private JenkinsItem getBuildDetail(final Job.Detail detail,
      final boolean lazyLoad) {
    JenkinsItem subItem = null;
    switch (detail) {
      case CHANGES:
        subItem = new JobChanges(job, this);
        break;
      case ARTIFACTS:
        subItem = new JobArtifacts(job, this);
        break;
      case FAILEDTESTS:
        subItem = new JobFailedTests(job, this);
        break;
      case INSTALL:
        subItem = new JobInstallPackage(job, this, lazyLoad);
        break;
      default:
        return null;
    }
    subItem.init(client());
    return subItem;
  }
}
