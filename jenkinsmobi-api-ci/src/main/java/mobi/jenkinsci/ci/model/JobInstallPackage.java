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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mobi.jenkinsci.ci.client.ArtifactFingerprint;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import org.apache.log4j.Logger;

public class JobInstallPackage extends JobBuildDetail {
  private static final Logger log = Logger.getLogger(JobInstallPackage.class);
  private final JobArtifacts artifacts;

  public JobInstallPackage(final Job job, final Build build, final boolean lazyLoad) {
    super(job, build);
    this.artifacts = new JobArtifacts(job, build);
    this.lazyLoad = lazyLoad;
  }

  public JobInstallPackage(final Job job) {
    this(job, job.lastCompletedBuild, false);
  }

  @Override
  public ItemNode toAbstractNode(final String jenkinsUrl) throws IOException {
    final ItemNode details = new ItemNode(Layout.LIST);
    details.setTitle("Download and Install");
    details.setIcon("?image=icons/download.png");
    details.setPath(Job.Detail.INSTALL.toString().toLowerCase());


    if (isAndroidCompatible()) {
      final ItemNode upgradeAction = artifacts.toAbstractNode(jenkinsUrl).getPayload().get(0);
      upgradeAction.setDescription("INSTALL");
      upgradeAction.setDescriptionColor("#0042A0");
      upgradeAction.setDescriptionAlign(Alignment.RIGHT);
      upgradeAction.setIcon("?image=icons/android-install.png");
      details.addNode(upgradeAction);
    }

    if(!lazyLoad) {
    details.addNodes(getChanges(jenkinsUrl));
    }
    details.setLeaf(true);
    return details;
  }

  public boolean isAndroidCompatible() {
    return artifacts.isAndroidCompatible();
  }

  private List<ItemNode> getChanges(final String jenkinsUrl)
 throws IOException {
    final ArrayList<ItemNode> changes = new ArrayList<ItemNode>();
    final int jobBuildNumber = build.number;

    final HashMap<URL, ItemNode> ticketsMap = new HashMap<URL, ItemNode>();
    final List<ItemNode> changesList = new ArrayList<ItemNode>();

    int remoteBuildNumber = 0;
    try {
      final ArtifactFingerprint remoteBuildFingerprint =
          client().getArtifactFromMD5(getApkMd5());
      remoteBuildNumber = remoteBuildFingerprint.original.number;
    } catch (final Exception e) {
      log.error("Cannot lookup APK fingerprint from Jenkins: returning ONLY the latest build changes / issues");
      remoteBuildNumber = build.number - 1;
    }

    for (final Build build : job.builds) {
      if (build.number > remoteBuildNumber && build.number <= jobBuildNumber) {
        for (final ChangeSetItem jobChange : client().getJobChanges(job.path,
            build.number).items) {

          if (jobChange.issue != null) {
            final URL ticketUrl = jobChange.issue.linkUrl;
            if (ticketsMap.get(ticketUrl) == null) {
              final ItemNode ticket = client().account.getPluginNodeForUrl(
                  client().config.pluginConfig, ticketUrl);
              ticketsMap.put(ticketUrl, ticket);
            }
          } else {
            changesList.add(jobChange.toAbstractNode(jenkinsUrl));
          }
        }
      }
    }

    if (!ticketsMap.isEmpty()) {
      changes.add(new HeaderNode("New features / Fixed bugs"));
      changes.addAll(ticketsMap.values());
    }

    if (!changesList.isEmpty()) {
      changes.add(new HeaderNode("Code changes"));
      changes.addAll(changesList);
    }
    return changes;
  }

  private String getApkMd5() {
    // TODO Auto-generated method stub
    return null;
  }
}
