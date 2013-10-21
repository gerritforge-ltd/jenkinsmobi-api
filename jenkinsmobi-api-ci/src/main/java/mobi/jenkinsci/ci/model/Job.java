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
import java.lang.reflect.Field;
import java.util.List;

import mobi.jenkinsci.ci.addon.Utils;
import mobi.jenkinsci.ci.client.JenkinsClient;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;

public class Job extends JobName {
  private static final Logger LOG = Logger.getLogger(Job.class);

  public boolean buildable;
  public String nextBuildNumber;
  public List<HealthReport> healthReport;
  public Build lastSuccessfulBuild;
  public Build lastCompletedBuild;
  public Build lastBuild;
  public Build lastStableBuild;
  public Build lastUnstBuildBuild;
  public List<MavenMobule> modules;
  public List<Build> builds;

  transient Plugin plugin;

  public enum Command {
    BUILD, STOP;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    };
  };

  public enum Detail {
    CHANGES, FAILEDTESTS, MODULES, ARTIFACTS, BUILDS, INSTALL;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    };
  }

  @Override
  public void init(final JenkinsClient client) {
    super.init(client);

    init(client, lastBuild);
    init(client, lastCompletedBuild);
    init(client, lastStableBuild);
    init(client, lastSuccessfulBuild);
    init(client, lastUnstBuildBuild);
    for (final Build build : builds) {
      init(client, build);
    }

    lazyLoad = true;
  }

  private void init(final JenkinsClient client, final Build build) {
    if (build == null) {
      return;
    }
    build.setJob(this);
    build.init(client);
  }



  public List<HealthReport> getHealthReport() {
    return healthReport;
  }

  public Build getLastCompletedBuild() {
    return lastCompletedBuild;
  }

  public List<MavenMobule> getModules() {
    return modules;
  }

  public String getNextBuildNumber() {
    return nextBuildNumber;
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    final ItemNode result = new ItemNode(Layout.LIST);
    result.setTitle(name);
    result.setViewTitle(name);
    result.setPath(getNodePathFromFullJenkinsURL(urlPrefix, url));
    result.setVersion(ItemNode.API_VERSION);

    Utils.setIconByLabel(result, color);

    ItemNode currentNode = new ItemNode();
    Utils.setIconByLabel(currentNode, color);
    currentNode.setPath("image");
    currentNode.setAction("?web=" + urlEncode(url));
    if (lastCompletedBuild != null) {
      currentNode.setTitle("Build #" + lastCompletedBuild.number);
    } else {
      currentNode.setTitle("Never built");
    }
    result.addNode(currentNode);

    if (lastCompletedBuild != null) {
      currentNode = new ItemNode();
      if (isBuilding()) {
        currentNode.setTitle("Build in progress from "
            + lastCompletedBuild.builtSinceHumanReadable());
      } else {
        currentNode.setTitle("Last build was "
            + lastCompletedBuild.builtSinceHumanReadable() + " and took "
            + lastCompletedBuild.durationeHumanReadable());
      }
      result.addNode(currentNode);
    }

    if (!isBuilding()) {
      if (getHealthReport() != null && getHealthReport().size() > 0) {
        currentNode = new ItemNode();
        currentNode.setTitle(getHealthReport().get(0).description);
        currentNode.setIcon(result.getPath() + "?image=icons/"
            + getHealthReport().get(0).getIconUrl());
        result.addNode(currentNode);
        if (getHealthReport().size() > 1) {
          currentNode = new ItemNode();
          currentNode.setIcon(result.getPath() + "?image=icons/"
              + getHealthReport().get(1).getIconUrl());
          currentNode.setTitle(getHealthReport().get(1).description);
          result.addNode(currentNode);
        }
      }
    }


    if (!isBuilding() && lastCompletedBuild != null) {
      final ItemNode h1 = new HeaderNode("Last build");
      result.addNode(h1);
      result.addNodes(lastBuild.getBuildSubNodes(this, urlPrefix, lazyLoad));
    }

    result.addNode(getDetail(Detail.MODULES).toAbstractNode(urlPrefix));

    result.addNode(new HeaderNode("History"));
    result.addNode(getDetail(Detail.BUILDS).toAbstractNode(urlPrefix));

    final ItemNode menuEntry = new ItemNode();
    if (isBuilding()) {
      menuEntry.setTitle("Stop build");
      menuEntry.setAction("?cmd=" + Command.STOP);
      result.addMenuNode(menuEntry);
    } else {
      menuEntry.setTitle("Start build");
      menuEntry.setAction("?cmd=" + Command.BUILD);
      result.addMenuNode(menuEntry);
    }
    result.setLeaf(true);

    return result;
  }

  @Override
  public boolean isBuilding() {
    if (builds == null || builds.size() <= 0) {
      return false;
    }

    return builds.get(0).building;
  }

  public JobDetail getDetail(final Detail detail) {
    switch (detail) {
      case ARTIFACTS:
        return new JobArtifacts(this);
      case CHANGES:
        return new JobChanges(this);
      case FAILEDTESTS:
        return new JobFailedTests(this);
      case MODULES:
        return new JobModules(this);
      case BUILDS:
        return new JobBuilds(this);
      case INSTALL:
        return new JobInstallPackage(this);
      default:
        throw new IllegalArgumentException("Unsupported job detail " + detail);
    }
  }

  public Build getBuild(final String buildName) {
    try {
      if (!Character.isDigit(buildName.charAt(0))) {
        final Field buildField = Job.class.getField(buildName);
        final Build build = (Build) buildField.get(this);
        return build;
      } else {
        final int buildNumber = Integer.parseInt(buildName);
        for (final Build build : builds) {
          if (build.number == buildNumber) {
            return build;
          }
        }
      }
    } catch (final Exception e) {
      LOG.error("Cannot find build " + buildName + " in job " + name);
      return null;
    }

    return null;
  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    final JenkinsItem subItem = super.getSubItem(plugin, subItemPath);
    if (Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }
    final JobDetail jobDetail =
        getDetail(Job.Detail.valueOf(getHeadPath(subItemPath).toUpperCase()));
    return jobDetail.getSubItem(plugin, getTailPath(subItemPath));
  }
}
