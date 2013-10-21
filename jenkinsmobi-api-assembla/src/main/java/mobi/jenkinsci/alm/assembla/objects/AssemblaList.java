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
import java.util.Collections;
import java.util.List;

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

public class AssemblaList<T extends AssemblaItem> extends AssemblaItem {
  public List<T> items;
  private final String listName;

  public AssemblaList(final String listName, final List<T> items) {
    if (items == null) {
      this.items = Collections.emptyList();
    } else {
      this.items = items;
    }
    this.listName = listName;
    this.path = UrlPath.normalizePath(listName);
  }

  @Override
  public AssemblaItem getSubNode(final UrlPath path,
      final boolean useAbsoluteNodePaths) throws IOException {
    if (path == null || path.isEmpty()) {
      return this;
    }

    final String head = path.getHead();
    for (final T item : items) {
      if (item.hasPath(head)) {
        final AssemblaItem node =
            item.getSubNode(path.getTail(), useAbsoluteNodePaths);
        if (useAbsoluteNodePaths) {
          node.applyPathPrefix(this.path);
        }
        return node;
      }
    }

    return new EmptyAssemblaItem(head);
  }

  @Override
  public ItemNode serializeToJenkinsCloudObjects() {
    final ItemNode result = new ItemNode();
    result.setPath(path);
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle(listName);
    result.setViewTitle(listName);
    result.setIcon(result.getPath() + "?image=icons/" + listName + ".png");

    for (final T space : items) {
      result.addNode(space.serializeToJenkinsCloudObjects());
    }

    return result;
  }

  @Override
  public AssemblaItem init(final AssemblaClient client) {
    super.init(client);

    for (final T item : items) {
      item.init(client);
    }

    return this;
  }

}
