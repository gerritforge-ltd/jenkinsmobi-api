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
package mobi.jenkinsci.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonWriter;

public class ItemNode extends AbstractNode {

  public final static String API_VERSION = "3.0";

  private static final boolean JSON_PRETTY_PRINTING = Boolean
      .getBoolean("JENKINS_CLOUD_JSON_PRETTY");

  @Getter
  @Setter
  protected Layout layout;
  @Getter
  @Setter
  protected List<ItemNode> payload;
  @Getter
  @Setter
  protected List<ItemNode> menu;
  @Getter
  @Setter
  private String version;
  @Getter
  protected String title;
  @Getter
  protected String description;
  @Getter
  @Setter
  protected Alignment descriptionAlign;
  @Getter
  @Setter
  protected String icon;
  @Getter
  @Setter
  protected Alignment iconAlign;
  @Getter
  @Setter
  protected String path;
  @Getter
  @Setter
  protected String titleColor;
  @Getter
  @Setter
  protected String descriptionColor;
  @Getter
  @Setter
  protected String action;
  @SerializedName("moredata")
  @Getter
  @Setter
  protected boolean hasMoreData;
  @SerializedName("viewtitle")
  @Getter
  @Setter
  protected String viewTitle;
  @Getter
  @Setter
  protected String timestamp;
  @Getter
  @Setter
  protected String modified;
  protected boolean preload;

  private transient ItemNode parent;

  public ItemNode(final String title, final String description,
      final String icon) {
    this(title, description);
    this.icon = icon;
  }

  public ItemNode(final String title, final String description) {
    this(title);
    this.description = description;
  }

  public ItemNode(final String title) {
    this();
    this.title = title;
  }

  public ItemNode(final Layout layout) {
    this();
    this.layout = layout;
  }

  public ItemNode() {
    this.httpContentType = "application/json; charset=utf-8";
  }

  public void addNode(final ItemNode node) {
    if (node == null) {
      return;
    }

    if (payload == null) {
      payload = new LinkedList<ItemNode>();
    }
    node.parent = this;
    payload.add(node);
  }

  public void addNode(final int index, final ItemNode node) {
    if (payload == null) {
      payload = new LinkedList<ItemNode>();
    }
    node.parent = this;
    payload.add(index, node);
  }

  public void setTitle(final String title) {
    this.title = getSafeString(title);
  }

  private String getSafeString(final String origString) {
    if (origString == null) {
      return "";
    }

    final byte[] origBytes = origString.getBytes();
    final byte[] outBytes = new byte[origBytes.length];

    for (int i = 0; i < outBytes.length; i++) {
      final byte currChar = origBytes[i];
      if ((currChar >= 32 && currChar <= 126) || (currChar == '\n')) {
        outBytes[i] = currChar;
      } else {
        outBytes[i] = ' ';
      }
    }

    return new String(outBytes);
  }


  public void setDescription(final String description) {
    this.description = getSafeString(description);
  }

  public String getAbsolutePath() {

    String result = path;
    if (path != null) {
      if (parent != null) {
        final String parentPath = parent.getAbsolutePath();
        if (parentPath != null) {
          result = parentPath + (parentPath.endsWith("/") ? "" : "/") + path;
        } else {
          if (!path.startsWith("/")) {
            result = "/" + path;
          }
        }
      } else {
        if (!path.startsWith("/")) {
          result = "/" + path;
        }
      }
    }
    return result;
  }


  public String getPath(final String root) {
    return path + root;
  }

  public void toJson(final PrintWriter writer) {
    getGson().toJson(this, getClass(), new JsonWriter(writer));
  }

  public String toJson() {
    final Gson gson = getGson();
    return gson.toJson(this);
  }

  private Gson getGson() {
    final GsonBuilder gbuilder = new GsonBuilder();
    gbuilder.disableHtmlEscaping();
    if (JSON_PRETTY_PRINTING) {
      gbuilder.setPrettyPrinting();
    }
    final Gson gson = gbuilder.create();
    return gson;
  }


  public InputStream toJsonStream() {

    final GsonBuilder gbuilder = new GsonBuilder();
    gbuilder.disableHtmlEscaping();

    final StringWriter writer = new StringWriter();
    gbuilder.create().toJson(this, writer);

    final InputStream result =
        new ByteArrayInputStream(writer.getBuffer().toString().getBytes());
    return result;
  }

  public void addMenuNode(final ItemNode menuNode) {

    if (menu == null) {

      menu = new LinkedList<ItemNode>();
    }

    menu.add(menuNode);
  }


  @Override
  public InputStream getDownloadedObjectData() {
    return toJsonStream();
  }

  @Override
  public String getDownloadedObjectType() {

    return "JenkinsCloudDataNode";
  }

  @Override
  public String getHttpContentType() {

    return "application/json";
  }

  public ItemNode duplicate() {
    final ItemNode copy = new ItemNode();
    copy.layout = layout;
    copy.version = version;
    copy.title = title;
    copy.description = description;
    copy.descriptionAlign = descriptionAlign;
    copy.icon = icon;
    copy.iconAlign = iconAlign;
    copy.path = path;
    copy.titleColor = titleColor;
    copy.descriptionColor = descriptionColor;
    copy.action = action;
    copy.hasMoreData = hasMoreData;
    copy.viewTitle = viewTitle;
    copy.timestamp = timestamp;
    copy.modified = modified;
    copy.eTag = eTag;
    copy.cacheable = cacheable;
    copy.preload = preload;
    copy.httpContentType = httpContentType;

    copy.payload = duplicateList(payload);
    copy.menu = duplicateList(menu);

    return copy;
  }

  private List<ItemNode> duplicateList(final List<ItemNode> origList) {
    if (origList == null) {
      return null;
    }

    final ArrayList<ItemNode> copyList = new ArrayList<ItemNode>();
    for (final ItemNode itemNode : origList) {
      copyList.add(itemNode.duplicate());
    }

    return copyList;
  }

  @Override
  public String toString() {
    return getClass() + "[" + getPath() + "," + getTitle() + "]";
  }

  public void addNodes(final List<ItemNode> buildSubNodes) {
    for (final ItemNode itemNode : buildSubNodes) {
      addNode(itemNode);
    }
  }

  @Override
  public void toStream(final OutputStream out) {
    @Cleanup
    final PrintWriter writer = new PrintWriter(out);
    toJson(writer);
  }

  public static Builder itemNodeBuilder(final String nodeTitle) {
    return new Builder(nodeTitle);
  }

  public static class Builder {
    private Layout layout;
    private List<ItemNode> payload;
    private List<ItemNode> menu;
    private String version;
    private String title;
    private String description;
    private Alignment descriptionAlign;
    private String icon;
    private Alignment iconAlign;
    private String path;
    private String titleColor;
    private String descriptionColor;
    private String action;
    private boolean hasMoreData;
    private String viewTitle;
    private String timestamp;
    private String modified;
    private boolean preload;
    private ItemNode parent;
    private String eTag;
    private boolean cacheable;

    private Builder(final String nodeTitle) {
      this.title = nodeTitle;
    }

    public Builder layout(final Layout layout) {
      this.layout = layout;
      return this;
    }

    public Builder payload(final ItemNode... payload) {
      this.payload = Arrays.asList(payload);
      return this;
    }

    public Builder menu(final ItemNode... menu) {
      this.menu = Arrays.asList(menu);
      return this;
    }

    public Builder version(final String version) {
      this.version = version;
      return this;
    }

    public Builder title(final String title) {
      this.title = title;
      return this;
    }

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder descriptionAlign(final Alignment descriptionAlign) {
      this.descriptionAlign = descriptionAlign;
      return this;
    }

    public Builder icon(final String icon) {
      this.icon = icon;
      return this;
    }

    public Builder iconAlign(final Alignment iconAlign) {
      this.iconAlign = iconAlign;
      return this;
    }

    public Builder path(final String path) {
      this.path = path;
      return this;
    }

    public Builder titleColor(final String titleColor) {
      this.titleColor = titleColor;
      return this;
    }

    public Builder descriptionColor(final String descriptionColor) {
      this.descriptionColor = descriptionColor;
      return this;
    }

    public Builder action(final String action) {
      this.action = action;
      return this;
    }

    public Builder hasMoreData(final boolean hasMoreData) {
      this.hasMoreData = hasMoreData;
      return this;
    }

    public Builder viewTitle(final String viewTitle) {
      this.viewTitle = viewTitle;
      return this;
    }

    public Builder timestamp(final String timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder modified(final String modified) {
      this.modified = modified;
      return this;
    }

    public Builder preload(final boolean preload) {
      this.preload = preload;
      return this;
    }

    public Builder eTag(String etag) {
      this.eTag = etag;
      return this;
    }

    public Builder parent(final ItemNode parent) {
      this.parent = parent;
      return this;
    }

    public Builder cacheable(boolean cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public ItemNode build() {
      return new ItemNode(this);
    }


  }

  private ItemNode(final Builder builder) {
    this.layout = builder.layout;
    this.menu = builder.menu;
    this.version = builder.version;
    this.title = builder.title;
    this.description = builder.description;
    this.descriptionAlign = builder.descriptionAlign;
    this.icon = builder.icon;
    this.iconAlign = builder.iconAlign;
    this.path = builder.path;
    this.titleColor = builder.titleColor;
    this.descriptionColor = builder.descriptionColor;
    this.action = builder.action;
    this.hasMoreData = builder.hasMoreData;
    this.viewTitle = builder.viewTitle;
    this.timestamp = builder.timestamp;
    this.modified = builder.modified;
    this.preload = builder.preload;
    this.parent = builder.parent;

    if (builder.payload != null) {
      for (final ItemNode childNode : builder.payload) {
        addNode(childNode);
      }
    }
    this.setETag(builder.eTag);
    this.setCacheable(builder.cacheable);
  }
}
