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

import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;

import com.google.common.base.Strings;

public class JobBuilds extends JobDetail {

  enum DateRange {
    TODAY("Today"), YESTERDAY("Yesterday"), WEEK("This week"), MONTH(
        "This month"), OLDER("Older"), FUTURE("Future");

    private String title;

    private DateRange(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  };

  public JobBuilds(final Job job) {
    super(job);
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    final ItemNode builds = new ItemNode("Build History");
    builds.setViewTitle(job.name + " Builds");
    builds.setLayout(Layout.LIST);
    builds.setPath(Job.Detail.BUILDS.toString());
    long latestBuildTs = 0L;
    for (final Build build : job.builds) {
      builds.addNode(getHeaderNode(latestBuildTs, build.timestamp));
      latestBuildTs = build.timestamp;
      final ItemNode buildNode = build.toAbstractNode(urlPrefix);
      buildNode.setPath("" + build.number);
      builds.addNode(buildNode);
    }
    builds.setLeaf(true);

    return builds;
  }

  private ItemNode getHeaderNode(final long latestBuildTs, final long currTs) {
    final DateRange lastRange = getRange(latestBuildTs);
    final DateRange currRange = getRange(currTs);
    return (lastRange == currRange ? null:new HeaderNode(currRange.toString()));
  }

  private DateRange getRange(final long latestBuildTs) {
    final JobCalendar nowCal = new JobCalendar(System.currentTimeMillis());
    final JobCalendar latestCal = new JobCalendar(latestBuildTs);

    if (nowCal.compareDays(latestCal) < 0) {
      return DateRange.FUTURE;
    } else if (nowCal.compareDays(latestCal) == 0) {
      return DateRange.TODAY;
    } else if (nowCal.compareDays(latestCal) < 2) {
      return DateRange.YESTERDAY;
    } else if (nowCal.compareWeeks(latestCal) == 0) {
      return DateRange.WEEK;
    } else if (nowCal.compareMonths(latestCal) == 0) {
      return DateRange.MONTH;
    } else {
      return DateRange.OLDER;
    }

  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    final JenkinsItem subItem = super.getSubItem(plugin, subItemPath);

    if(Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }
    final String buildString = getHeadPath(subItemPath);
    if(buildString == null) {
      return subItem;
    }

    final int buildNumber = Integer.parseInt(buildString);
    final Build build = getBuild(buildNumber);
    return build.getSubItem(plugin, getTailPath(subItemPath));
  }

  private Build getBuild(final int buildNumber) {
    for (final Build build : job.builds) {
      if(build.number == buildNumber) {
        return build;
      }
    }
    return null;
  }
}
