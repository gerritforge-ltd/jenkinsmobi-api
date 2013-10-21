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

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

public class AssemblaSpace extends AssemblaItem {
  public String wiki_name;
  public String name;

  @Override
  public AssemblaItem init(final AssemblaClient client) {
    super.init(client);
    path = UrlPath.normalizePath(name);
    return this;
  }

  @Override
  public AssemblaItem getSubNode(final UrlPath path,
      final boolean useAbsolutePaths) throws IOException {
    if (path == null || path.isEmpty()) {
      return getSpaceTickets();
    }

    final AssemblaItem node =
        getSpaceTickets().getSubNode(path.getTail(), useAbsolutePaths);
    if (useAbsolutePaths) {
      node.applyPathPrefix(this.path);
    }
    return node;
  }

  private AssemblaTickets getSpaceTickets() throws IOException {
    return client.getTickets(id);
  }

  @Override
  public ItemNode serializeToJenkinsCloudObjects() {
    final ItemNode tickets = new ItemNode(Layout.LIST);
    tickets.setTitle(name);
    tickets.setPath(path);
    return tickets;
  }
}
