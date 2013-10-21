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
package mobi.jenkinsci.alm.assembla.objects;

import java.io.IOException;
import java.io.InputStream;

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.net.UrlPath;

public abstract class AssemblaItem {
  public String description;
  public String id;

  protected AssemblaClient client;
  protected String path;
  protected String prefix;

  public ItemNode serializeToJenkinsCloudObjects() {
    return null;
  }

  public AssemblaItem getSubNode(final UrlPath pathHelper,
      final boolean useAbsoluteNodePaths) throws IOException {
    if (pathHelper == null || pathHelper.isEmpty()) {
      return this;
    } else {
      return new EmptyAssemblaItem(pathHelper.getHead());
    }
  }

  public boolean hasPath(final String path) {
    return this.path.equalsIgnoreCase(path);
  }

  public AssemblaItem init(final AssemblaClient client) {
    this.client = client;
    return this;
  }

  public AssemblaItem post(final InputStream data, final String contentType)
      throws IOException {
    throw new IOException("Method not supported");
  }

  public void applyPathPrefix(final String pathPrefix) {
    path = applyPrefix(pathPrefix, path);
    prefix = applyPrefix(pathPrefix, prefix);
  }

  private String applyPrefix(final String pathPrefix, String path) {
    if (path == null) {
      path = "";
    }
    if (path.startsWith(pathPrefix) || path.startsWith("/")) {
      return path;
    }

    if (pathPrefix.endsWith("$")) {
      return pathPrefix + path;
    } else {
      return pathPrefix + "/" + path;
    }
  }
}
