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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import mobi.jenkinsci.ci.client.JenkinsClient;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;

import com.google.gson.Gson;

public abstract class JenkinsItem {

  public String description;
  public String name;
  public String url;
  public String color;
  public String displayName;
  public String path;

  transient protected JenkinsClient client;
  transient protected Plugin plugin;
  transient protected boolean lazyLoad;

  protected JenkinsClient client() {
    return client;
  }

  public ItemNode toAbstractNode(final String urlPrefix) throws IOException {
    return null;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public boolean equals(final Object obj) {

    boolean result = false;

    if (obj != null && obj instanceof JenkinsItem) {

      final JenkinsItem __obj = (JenkinsItem) obj;
      result = __obj.name.equals(this.name);
    }

    return result;
  }

  public void init(final JenkinsClient client) {
    final String urlPrefix = client.config.getUrl();
    this.path = getNodePathFromFullJenkinsURL(urlPrefix, url);
    this.url = replaceContextRoot(urlPrefix, url);
    this.client = client;
    this.lazyLoad = true;
  }

  protected String urlEncode(final String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      // This case will never happen as UTF-8 is hardcoded
      e.printStackTrace();
      return url;
    }
  }

  protected String getNodePathFromFullJenkinsURL(String prefix,
      String jenkinsUrl) {
    if (jenkinsUrl == null) {
      return JenkinsClient.VIEWS_ALL;
    }

    if (jenkinsUrl.startsWith("http") && prefix.startsWith("http")) {
      jenkinsUrl = getUrlPath(jenkinsUrl);
      prefix = getUrlPath(prefix);
    }
    if (jenkinsUrl.startsWith(prefix)) {
      jenkinsUrl = jenkinsUrl.substring(prefix.length());
    }

    if (jenkinsUrl.startsWith("/")) {
      jenkinsUrl = jenkinsUrl.substring(1);
    }
    if (jenkinsUrl.endsWith("/")) {
      jenkinsUrl = jenkinsUrl.substring(0, jenkinsUrl.length() - 1);
    }

    final String[] urlParts = jenkinsUrl.split("/");
    if (urlParts == null || urlParts.length <= 0
        || (urlParts.length == 1 && urlParts[0].trim().length() <= 0)) {
      return JenkinsClient.VIEWS_ALL;
    } else {
      return urlParts[urlParts.length - 1];
    }
  }

  private String getUrlPath(final String urlString) {
    try {
      return new URL(urlString).getPath();
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException("Invalid path for malformed URL "
          + urlString, e);
    }
  }

  protected String replaceContextRoot(final String prefix,
      final String jenkinsUrl) {
    if (jenkinsUrl == null) {
      return prefix;
    }

    if (!jenkinsUrl.startsWith("http") || !prefix.startsWith("http")) {
      return jenkinsUrl;
    }

    String jenkinsUrlPath = getUrlPath(jenkinsUrl);
    final String prefixPath = getUrlPath(prefix);

    if (jenkinsUrlPath.startsWith(prefixPath)) {
      jenkinsUrlPath = jenkinsUrlPath.substring(prefixPath.length());
      return prefix + jenkinsUrlPath;
    } else {
      return jenkinsUrl;
    }
  }

  public JenkinsItem getSubItem(final Plugin plugin, final String subItemPath)
      throws IOException {
    this.plugin = plugin;
    return this;
  }

  protected String getHeadPath(final String subItemPath) {
    final int slashPos = subItemPath.indexOf('/');
    if (slashPos > 0) {
      return subItemPath.substring(0, slashPos);
    } else {
      return subItemPath;
    }
  }

  protected String getTailPath(final String subItemPath) {
    final int slashPos = subItemPath.indexOf('/');
    if (slashPos > 0) {
      return subItemPath.substring(slashPos + 1);
    } else {
      return null;
    }
  }
}
