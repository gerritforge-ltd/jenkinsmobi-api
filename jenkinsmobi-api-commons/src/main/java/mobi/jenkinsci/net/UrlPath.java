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
package mobi.jenkinsci.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;

public class UrlPath {

  public static final int INFINITE_DEPTH = 99999;
  public static final int SINGLE_NODE_ONLY = 0;

  private boolean isRootPath = false;
  private String path;
  private String queryString;
  private int depth;
  private final List<String> components;
  private String requestPath;

  public UrlPath(final String basePath, final String subPath) {
    this(basePath
        + ((!basePath.endsWith("/") && !subPath.startsWith("/")) ? "/" : "")
        + subPath);
  }

  public UrlPath(final HttpServletRequest request) {
    this(request.getPathInfo());
  }

  public UrlPath(String requestedPath) {
    this.requestPath = requestedPath;

    if (requestedPath == null || requestedPath.trim().length() <= 0) {
      requestedPath = "/";
    } else if (requestedPath.startsWith("http")) {
      requestedPath = getUriRequestPath(requestedPath);
    } else if (!requestedPath.startsWith("/")) {
      requestedPath = "/" + requestedPath;
    }

    this.path = requestedPath;
    final StringTokenizer tokens = new StringTokenizer(path, "/");
    components = new LinkedList<String>();
    String nextToken;
    while (tokens.hasMoreTokens()) {
      nextToken = tokens.nextToken();
      if (nextToken != null) {
        components.add(nextToken);
      }
    }

    if (components.size() > 0) {
      isRootPath = components.size() == 1 && components.get(0).startsWith("_");
      depth = calculateDepth();
      path = removeDepthSuffix();
    } else {
      isRootPath = true;
      depth = SINGLE_NODE_ONLY;
    }
  }

  private String getUriRequestPath(String requestUri) {
    try {
      requestUri = new URI(requestUri).getPath();
      return requestUri;
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException("Malformed request URI " + requestUri,
          e);
    }
  }

  public UrlPath(final List<String> subList) {
    components = subList;
    path = buildPathFromComponents(subList);
    queryString = "";
  }

  private String buildPathFromComponents(final List<String> subList) {
    final StringBuilder pathBuilder = new StringBuilder();
    for (final String string : subList) {
      if (pathBuilder.length() > 0) {
        pathBuilder.append("/");
      }
      pathBuilder.append(string);
    }
    return pathBuilder.toString();
  }

  public String getPath(final int startURIComponent) {

    final StringBuilder result = new StringBuilder();

    if (startURIComponent >= components.size()) {
      result.append("/");
    } else {
      for (int i = startURIComponent; i < components.size(); i++) {

        result.append("/");
        result.append(components.get(i));
        if (components.get(i).endsWith("http:")) {
          result.append("/");
        }
      }
    }
    return result.toString();
  }

  public String getPath() {
    return path;
  }

  public List<String> getComponents() {
    return components;
  }

  public String getPathSuffix(final String startComponent) {
    final StringBuilder pathSuffix = new StringBuilder();
    boolean startComponentFound = false;
    for (final String component : components) {
      if (startComponentFound) {
        if (pathSuffix.length() > 0) {
          pathSuffix.append("/");
        }
        pathSuffix.append(component);
      } else {
        startComponentFound = component.equals(startComponent);
      }
    }
    return pathSuffix.toString();
  }

  public boolean isRootPath() {
    return isRootPath;
  }

  /**
   *
   * @param root The root node to start from
   * @return The node corresponding to this path
   * @throws ResourceNotFoundException
   */
  public AbstractNode followPath(final ItemNode root)
      throws ResourceNotFoundException {

    final AbstractNode result = root;

    if (isPath(root.getPath())) {
      return result;
    }

    if (root.getPayload() != null) {
      boolean following = false;
      for (final ItemNode child : root.getPayload()) {
        if (isPath(child.getAbsolutePath())) {
          following = true;
          return child;
        } else if (isFollowingPath(child.getAbsolutePath())) {
          following = true;
          return followPath(child);
        }
      }
      if (!following) {
        throw new ResourceNotFoundException("Cannot follow path structure of "
            + root);
      }
    }

    return result;
  }

  /**
   *
   * @param root The root node to start from
   * @return The node corresponding to this path
   */
  public AbstractNode cutTreeToDepth(final ItemNode root) {

    final ItemNode result = root.duplicate();

    if (result.getPayload() != null) {

      for (final ItemNode n : result.getPayload()) {

        cutTreeToDepth_Ric(n, depth - 1);
      }
    }

    return result;
  }

  private void cutTreeToDepth_Ric(final ItemNode n, final int depth) {

    if (depth < 0) {
      n.setPayload(null);
      n.setLayout(null);
      n.setMenu(null);
      n.setVersion(null);
    } else {

      if (n.getPayload() != null) {
        for (final ItemNode _n : n.getPayload()) {

          cutTreeToDepth_Ric(_n, depth - 1);
        }
      }
    }
  }

  /**
   *
   * @param __path The path to compare with this path I.e. if this.path =
   *        res1/res2/res3 and current node path is res1/res2 : the method
   *        should return true if this.path = res1/res2/res3 and current node
   *        path is re ss1/res4 : the method should return false
   * @return True if the given path is part of this path
   */
  public boolean isFollowingPath(String currentNodePath) {

    if (currentNodePath == null) {
      return false;
    }

    if (!currentNodePath.endsWith("/")) {
      currentNodePath = currentNodePath + "/";
    }

    if (!path.endsWith("/")) {
      path = path + "/";
    }

    final String currentNodePathPattern =
        "/?" + Matcher.quoteReplacement(currentNodePath) + ".*";
    return Pattern.matches(currentNodePathPattern, path);
  }

  /**
   *
   * @param __path The path to compare with this path
   * @return True if the given path full matches this path
   */
  public boolean isPath(final String __path) {

    final String pattern = "/?" + __path + "/?";
    return Pattern.matches(pattern, path);
  }

  public int getDepth() {
    return depth;
  }

  private int calculateDepth() {

    int result = SINGLE_NODE_ONLY;

    if (components.size() > 0) {
      final String potentialDepth = components.get(components.size() - 1);

      if (potentialDepth.startsWith("_")) {
        final String depth = potentialDepth.substring(1);
        if ("all".equals(depth)) {
          result = INFINITE_DEPTH;
        } else {
          result = Integer.parseInt(depth);
        }
      }
    }

    return result;
  }

  private String removeDepthSuffix() {

    String result = path;

    if (components.size() > 0) {
      final String potentialDepth = components.get(components.size() - 1);

      if (potentialDepth.startsWith("_")) {
        result = path.substring(0, path.length() - potentialDepth.length());
      }

      if (result.endsWith("/")) {

        result.substring(0, result.length() - 1);
      }
    }

    return result;
  }

  public static String getRealNodePath(final String instanceName,
      final ItemNode node) {

    return instanceName + "/" + node.getPath();
  }

  public static String normalizePath(final String path) {

    String result = path.toLowerCase();
    result = result.replaceAll("[*?@ |<>,/]", "_");
    return result;
  }

  public String getQueryString() {
    return queryString;
  }

  public void setQueryString(final String queryString) {
    this.queryString = queryString;
  }

  @Override
  public String toString() {
    return "path=" + path + " query=" + queryString;
  }

  public String getHead() {
    if (components.size() <= 0) {
      return null;
    } else {
      return components.get(0);
    }
  }

  public UrlPath getTail() {
    if (components.size() <= 0) {
      return null;
    } else {
      return new UrlPath(components.subList(1, components.size()));
    }
  }

  public boolean isEmpty() {
    return components == null || components.size() <= 0;
  }

  public String getPluginId() {
    final List<String> pathParts = getComponents();
    if (pathParts.size() <= 0) {
      return null;
    }
    final String firstPath = pathParts.get(0);
    return firstPath.indexOf("$") >= 0 ? StringUtils.substringBefore(firstPath,
        "$") : null;
  }

  public boolean isUpgradePath() {
    return requestPath != null && requestPath.startsWith("/upgrade");
  }
}
