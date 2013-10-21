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
import java.util.List;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;

import com.google.common.base.Strings;

public class JobChanges extends JobBuildDetail {

  public JobChanges(final Job job) {
    super(job, "lastCompletedBuild");
  }

  public JobChanges(final Job job, final Build build) {
    super(job, build);
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) {
    final ItemNode changes = new ItemNode("Code changes");
    changes.setPath(Job.Detail.CHANGES.toString());
    changes.setIcon("?image=icons/notepad.png");
    changes.setLayout(Layout.LIST);
    changes.setVersion(ItemNode.API_VERSION);
      changes.setViewTitle("Build #" + build.number + " changes");

    if (build.changeSet != null
        && build.changeSet.items != null) {
      final List<ChangeSetItem> items =
          build.changeSet.items;
      for (final ChangeSetItem item : items) {
        item.path = item.getUniqueId();
        changes.addNode(item.toAbstractNode(urlPrefix));
      }
    } else {
      changes.addNode(new ItemNode("No change"));
    }
    changes.setLeaf(true);
    return changes;
  }

  public ChangeSetItem getChangeSet(final String changeId) {
    if(build == null || build.changeSet == null ||
        build.changeSet.items == null) {
      return null;
    }

    for (final ChangeSetItem changeSet : build.changeSet.items) {
      if(changeSet.getUniqueId().equals(changeId)) {
        return changeSet;
      }
    }

    return null;
  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    final JenkinsItem subItem = super.getSubItem(plugin, subItemPath);

    if(Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }
    final ChangeSetItem changeItem = getChangeSet(getHeadPath(subItemPath));
    return changeItem.getSubItem(plugin, getTailPath(subItemPath));
  }
}
