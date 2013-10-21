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

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

public class AssemblaTicketComment extends AssemblaItem {
  public int ticket_id;
  public String comment;
  public GregorianCalendar created_on;
  public GregorianCalendar updated_at;
  public String user_id;

  private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

  @Override
  public boolean hasPath(final String path) {
    return true;
  }

  @Override
  public ItemNode serializeToJenkinsCloudObjects() {
    final ItemNode commentNode = new ItemNode(Layout.LIST);
    commentNode.setPath(id);
    commentNode.setViewTitle("Comment on #" + ticket_id);
    commentNode.addNode(new ItemNode(comment));

    final ItemNode createdDate = new ItemNode("Comment created at");
    createdDate.setDescription(dateFmt.format(created_on.getTime()));
    createdDate.setDescriptionAlign(Alignment.BOTTOM);
    commentNode.addNode(createdDate);

    return commentNode;
  }
}
