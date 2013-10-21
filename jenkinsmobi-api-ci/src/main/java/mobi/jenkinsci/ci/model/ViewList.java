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

import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;

public class ViewList extends JenkinsItem {

  @SerializedName("views")
  private List<View> views;

  @SerializedName("primaryView")
  private View primaryView;

  public List<View> getViews() {
    return views;
  }

  public JenkinsItem getPrimaryView() {
    return primaryView;
  }

  public void setViews(final List<View> views) {
    this.views = views;
  }

  public void setPrimaryView(final View primaryView) {
    this.primaryView = primaryView;
  }

  @Override
  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {

    final ItemNode result = new ItemNode();
    result.setPath(path);
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle("Builds");
    result.setIcon(result.getPath()+"?image=icons/views_wall.png");

    for (final JenkinsItem view : views) {
      if (view.name.equals(primaryView.name)) {
        final HeaderNode header =
            new HeaderNode("Primary view");
        final HeaderNode header2 =
            new HeaderNode("Other views");
        result.addNode(0, header);
        result.addNode(1, view.toAbstractNode(urlPrefix));
        result.addNode(2, header2);
      } else {
        result.addNode(view.toAbstractNode(urlPrefix));
      }
    }
    result.setLeaf(true);

    return result;
  }

  @Override
  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    final JenkinsItem subItem = super.getSubItem(plugin, subItemPath);

    if(Strings.isNullOrEmpty(subItemPath)) {
      return subItem;
    }

    final JenkinsItem viewDetails = client().getVewDetails(getHeadPath(subItemPath));
    return viewDetails.getSubItem(plugin, getTailPath(subItemPath));
  }
}
