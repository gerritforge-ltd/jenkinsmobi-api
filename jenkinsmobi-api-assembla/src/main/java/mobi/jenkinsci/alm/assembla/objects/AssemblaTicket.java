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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.GregorianCalendar;

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class AssemblaTicket extends AssemblaItem {
  private static final String CONTENT_TYPE_CHARSET_PREFIX = "charset=";
  private static final Logger LOG = Logger.getLogger(AssemblaTicket.class);
  public int number;
  public String summary;
  public int priority;
  public String reporterId;
  public GregorianCalendar completed_date;
  public GregorianCalendar created_on;
  public int permission_type;
  public int importance;
  public boolean is_story;
  public String milestone_id;
  public String notification_list;
  public String space_id;
  public int state;
  public String status;
  public int story_importance;
  public GregorianCalendar updated_at;
  public int working_hours;
  public int estimate;
  public int total_estimate;
  public int total_invested_hours;
  public int total_working_hours;
  public String assigned_to_id;
  public String reporter_id;

  private static class Priority {
    public final String label;
    public final String color;

    public Priority(final String label, final String color) {
      this.label = label;
      this.color = color;
    }
  }

  private static final Priority[] PRIORITIES = {
    null,
    new Priority("Highest", "#b80600"),
    new Priority("High", "#eb6400"),
    new Priority("Normal", "#009200"),
    new Priority("Low", "#676767"),
    new Priority("Lowest", "#bcbcbc")
  };

  @Override
  public AssemblaItem init(final AssemblaClient client) {
    super.init(client);
    path = "tickets/" + number;
    return this;
  }

  @Override
  public ItemNode serializeToJenkinsCloudObjects() {
    final ItemNode node = new ItemNode(Layout.LIST);
    node.setTitle("#" + number + " " + summary);
    node.setPath(path);
    node.setViewTitle("Ticket #" + number);
    node.setIcon(prefix  +"?image=icons/ico-ticket.png");

    node.addNode(new ItemNode(summary));
    node.addNode(new ItemNode(description));
    addFieldNode(node, "Status", status, null);
    addFieldNode(node, "Priority", PRIORITIES[priority].label, PRIORITIES[priority].color);


    node.addNode(new HeaderNode("Provide feedback"));
    try {
      ItemNode subnode = new ItemNode("I liked it, suggest new ideas");
      subnode.setIcon(prefix + "?image=icons/idea.png");
      subnode.setAction("post:/?dialog="
          + URLEncoder.encode("Tell us what you LIKED THE MOST, suggest new ideas on this feature !", "UTF-8"));
      subnode.setPath("idea");
      node.addNode(subnode);

      subnode = new ItemNode("Report problem");
      subnode.setIcon("?image=icons/problem.png");
      subnode.setAction("post:/?dialog="
          + URLEncoder.encode("Describe the problem you have experienced"
              + " and how to reproduce it", "UTF-8"));
      subnode.setPath("problem");
      node.addNode(subnode);

    } catch (final UnsupportedEncodingException e) {
      // This should never happen, as UTF-8 is hardcoded
      LOG.error("UTF-8 not supported", e);
    }

    return node;
  }

  private void addFieldNode(final ItemNode node, final String fieldName, final int fieldValue, final String color) {
    addFieldNode(node, fieldName, "" + fieldValue, color);
  }

  private void addFieldNode(final ItemNode node, final String fieldName, final String fieldValue, final String color) {
    final ItemNode fieldNode = new ItemNode(fieldName);
    fieldNode.setDescription(fieldValue);
    fieldNode.setDescriptionAlign(Alignment.RIGHT);
    if(color != null) {
    fieldNode.setDescriptionColor(color);
    }
    node.addNode(fieldNode);
  }

  public AssemblaItem getSubNode(final UrlPath pathHelper) throws Exception {
      return this;
  }

  @Override
  public AssemblaItem post(final InputStream data, final String contentType)
      throws IOException {
    final String[] contentTypeParts = contentType.split(";");
    if(contentTypeParts.length <= 0) {
      throw new IOException("Empty content type");
    }
    String charSet = "UTF-8";

    if(!contentTypeParts[0].trim().equalsIgnoreCase("text/plain")) {
      throw new IOException(contentTypeParts[0] + " Content-Type is NOT supported");
    }

    if(contentTypeParts.length > 1) {
      final String charSetPart = contentTypeParts[1].trim();
      if(charSetPart.startsWith(CONTENT_TYPE_CHARSET_PREFIX)) {
         charSet=charSetPart.substring(CONTENT_TYPE_CHARSET_PREFIX.length());
      }
    }

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(data, out);
    final String postString = out.toString(charSet);

    return client.addComment(space_id, number, postString);
  }

  @Override
  public boolean hasPath(final String path) {
    if (super.hasPath(path)) {
      return true;
    } else {
      return this.path.substring(this.path.indexOf('/') + 1).equals(path);
    }
  }

  @Override
  public AssemblaItem getSubNode(final UrlPath pathHelper,
      final boolean useAbsoluteNodePaths) {
    return this;
  }

}
