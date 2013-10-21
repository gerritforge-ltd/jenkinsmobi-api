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

import java.util.Arrays;
import java.util.List;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

public class JobArtifacts extends JobBuildDetail {

  public JobArtifacts(final Job job) {
    super(job, "lastCompletedBuild");
  }

  public JobArtifacts(final Job job, final Build build) {
    super(job, build);
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) {
    final ItemNode artifacts = new ItemNode("Artifacts");
    artifacts.setPath(Job.Detail.ARTIFACTS.toString());
    artifacts.setLayout(Layout.LIST);
    artifacts.setIcon("?image=icons/artifact.png");
    artifacts.setVersion(ItemNode.API_VERSION);
    artifacts.setViewTitle("Build #" + build.number + " artifacts");

    // artifacts
    if (build != null && build.artifacts != null) {
      artifacts.setLayout(Layout.LIST);
      for (final Artifact artifact : build.artifacts) {
        artifact.path =
            build.url + "/artifact/"
                + artifact.getRelativePath();
        artifacts.addNode(artifact.toAbstractNode(urlPrefix));
      }
    }
    artifacts.setLeaf(true);
    return artifacts;
  }

  public List<Artifact> getArtifacts() {
    if(build == null) {
      return Arrays.asList(new Artifact[] {});
    } else {
      return build.artifacts;
    }
  }

  public boolean isAndroidCompatible() {
    final List<Artifact> artifacts = getArtifacts();
    return artifacts.size() > 0 && artifacts.get(0).getRelativePath().endsWith(".apk");
  }

}
