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
package mobi.jenkinsci.ci;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.ci.client.ArtifactFingerprint;
import mobi.jenkinsci.ci.client.JenkinsClient;
import mobi.jenkinsci.ci.model.Build;
import mobi.jenkinsci.ci.model.ChangeSetItem;
import mobi.jenkinsci.ci.model.ComputerList;
import mobi.jenkinsci.ci.model.FailedTestsList;
import mobi.jenkinsci.ci.model.JenkinsItem;
import mobi.jenkinsci.ci.model.Job;
import mobi.jenkinsci.ci.model.Queue;
import mobi.jenkinsci.ci.model.ViewList;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.URLDownloader;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;

public class JenkinsCIPlugin implements Plugin, URLDownloader {
  private static final Pattern JOB_PATTERN = Pattern.compile("/job/([^/]+)");
  private static final Pattern JOB_BUILD_PATTERN = Pattern
      .compile("/job/([^/]+)/([^/]+)");
  private static final Pattern APK_MD5_PATTERN = Pattern
      .compile("APK-MD5/([0-9a-f]+)");
  private static final List<String> HEADERS_BLACKLIST = Arrays
      .asList(new String[] {HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST,
          HttpHeaders.AUTHORIZATION, HttpHeaders.CONNECTION, "Cookie",
          "Origin", HttpHeaders.REFERER, HttpHeaders.ACCEPT_ENCODING,
          HttpHeaders.CONTENT_ENCODING});

  @Override
  public AbstractNode processRequest(final Account account,
      final HttpServletRequest req, final PluginConfig pluginConf)
      throws IOException {

    final JenkinsClient client = JenkinsClient.getInstance(account, pluginConf);
    final UrlPath reqPath = new UrlPath(req.getRequestURI());

    final String command = req.getParameter("cmd");
    if (command != null) {
      return executeCommand(command, pluginConf, client, reqPath);
    } else {
      return getNode(account, pluginConf, reqPath);
    }
  }

  private AbstractNode executeCommand(final String command,
      final PluginConfig pluginConf, final JenkinsClient client,
      final UrlPath reqPath) throws IOException {
    if (command.startsWith("load:/")) {
      final String url = command.substring("load:/".length());
      if (url.endsWith("testReport")) {
        final FailedTestsList list = client.getFailedTestsList();
        return list.toAbstractNode(pluginConf.getUrl());
      } else {
        return null;
      }
    } else {
      return client.execute(reqPath, command);
    }
  }

  @Override
  public String validateConfig(final HttpServletRequest req,
      final Account account, final PluginConfig pluginConf)
      throws TwoPhaseAuthenticationRequiredException {
    try {
      final JenkinsClient client =
          JenkinsClient.getInstance(account, pluginConf);
      final ViewList views = client.getViewList(req);
      if (views == null || views.getViews() == null
          || views.getViews().size() <= 0) {
        return "Unable to retrieve views from Jenkins at "
            + pluginConf.getUrl();
      }
    } catch (final MalformedURLException e) {
      return "Invalid Jenkins Server URL:\n" + pluginConf.getUrl();
    } catch (final TwoPhaseAuthenticationRequiredException e) {
      throw e;
    } catch (final IOException e) {
      final String localizedMessage = e.getLocalizedMessage();
      return "Error contacting Jenkins at " + pluginConf.getUrl()
          + (localizedMessage != null ? "\n" + localizedMessage : "");
    }
    return null;
  }


  @Override
  public RawBinaryNode download(final HttpServletRequest req, final String url, final Account account, final PluginConfig pluginConf)
      throws IOException {
    final JenkinsClient client = JenkinsClient.getInstance(account, pluginConf);
    final HttpRequestBase get = getNewHttpRequest(req, url);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final RawBinaryNode result = new RawBinaryNode();
    try {
      final HttpResponse response = client.http.execute(get);

      final StatusLine status = response.getStatusLine();
      if (status.getStatusCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("HTTP-" + get.getMethod() + " " + url
            + " failed: status code " + status);
      }
      IOUtils.copy(response.getEntity().getContent(), out);
      for (final Header h : response.getAllHeaders()) {
        final String headerName = h.getName();
        if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
          result.setHttpContentType(h.getValue());
        } else if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
          result.setSize(Long.parseLong(h.getValue()));
        } else if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_ENCODING)) {
          result.setHttpCharacterEncoding(h.getValue());
        }
      }
    } finally {
      get.releaseConnection();
    }
    result.setData(new ByteArrayInputStream(out.toByteArray()));
    return result;
  }

  private HttpRequestBase getNewHttpRequest(final HttpServletRequest req,
      final String url) throws IOException {
    HttpRequestBase newReq = null;
    if (req == null || req.getMethod().equalsIgnoreCase("get")) {
      newReq = new HttpGet(url);
    } else {
      final HttpPost post = new HttpPost(url);
      final ByteArrayOutputStream reqBody = new ByteArrayOutputStream();
      copyHeaders(post, req);
      IOUtils.copy(req.getInputStream(), reqBody);
      post.setEntity(new ByteArrayEntity(reqBody.toByteArray()));
      newReq = post;
    }

    return newReq;
  }

  private void copyHeaders(final HttpPost post, final HttpServletRequest req) {
    for (final Enumeration<String> headerNames = req.getHeaderNames(); headerNames
        .hasMoreElements();) {
      final String headerName = headerNames.nextElement();
      if (HEADERS_BLACKLIST.contains(headerName)) {
        continue;
      }
      post.setHeader(headerName, req.getHeader(headerName));
    }
  }

  protected byte[] retrieveUrl(final String userAgent, final String linkUrl,
      final HashMap<String, HeaderElement[]> contentHeaders, final Object ctx)
      throws Exception {
    final JenkinsClient client = (JenkinsClient) ctx;
    final HttpRequestBase get = getNewHttpRequest(null, linkUrl);
    if (userAgent != null) {
      get.setHeader("User-Agent", userAgent);
    }
    final HttpResponse response = client.http.execute(get);
    try {
      final int status = response.getStatusLine().getStatusCode();
      if (status != HttpURLConnection.HTTP_OK) {
        throw new IOException("HTTP- " + get.getMethod() + " " + linkUrl
            + " returned status " + status);
      }

      if (contentHeaders != null) {
        for (final Header header : response.getAllHeaders()) {
          contentHeaders.put(header.getName(), header.getElements());
        }
      }

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final InputStream content = response.getEntity().getContent();
      IOUtils.copy(content, out);
      content.close();
      return out.toByteArray();
    } finally {
      get.releaseConnection();
    }
  }

  public ItemNode getNode(final Account account, final PluginConfig pluginConf,
      final UrlPath pathHelper) throws IOException {
    final JenkinsClient client = JenkinsClient.getInstance(account, pluginConf);
    final String urlPrefix = pluginConf.getUrl();
    final ItemNode rootNode = new ItemNode();

    JenkinsItem viewList = null;
    if (pathHelper.isRootPath() || pathHelper.isFollowingPath("views")) {
      viewList = getViewJobNode(pathHelper, client);
      if (!pathHelper.isRootPath()) {
        return viewList.toAbstractNode(urlPrefix);
      }
    }

    ComputerList computerList = null;
    if (pathHelper.isRootPath() || pathHelper.isFollowingPath("computer")) {
      computerList = client.getComputerList();
    }

    Queue queue = null;
    if (pathHelper.isRootPath() || pathHelper.isFollowingPath("queue")) {
      queue = client.getQueue();
    }

    if (pathHelper.isRootPath()) {
      rootNode.setLayout(Layout.ICONS);
    }

    rootNode.setPath("/");
    rootNode.setVersion(ItemNode.API_VERSION);

    if (viewList != null) {
      final ItemNode node = viewList.toAbstractNode(urlPrefix);
      rootNode.addNode(node);
    }

    if (computerList != null) {
      final ItemNode node = computerList.toAbstractNode(urlPrefix);
      rootNode.addNode(node);
    }

    if (queue != null) {
      final ItemNode node = queue.toAbstractNode(urlPrefix);
      rootNode.addNode(node);
    }

    return rootNode;
  }

  public JenkinsItem getViewJobNode(final UrlPath pathHelper,
      final JenkinsClient client) throws IOException {
    final String viewPathComponents = pathHelper.getPathSuffix("views");
    return client.getViewList().getSubItem(this, viewPathComponents);
  }

  @Override
  public void init() {
  }

  @Override
  public String getType() {
    return "JenkinsCI";
  }

  @Override
  public List<ItemNode> getEntryPoints(final PluginConfig pluginConf)
      throws IOException {
    final List<ItemNode> result = new LinkedList<ItemNode>();

    ItemNode entryPoint = new ItemNode();
    entryPoint.setTitle("Builds");
    entryPoint.setPath("views");
    entryPoint.setIcon("?image=icons/views_wall.png");
    result.add(entryPoint);

    entryPoint = new ItemNode();
    entryPoint.setTitle("Nodes");
    entryPoint.setPath("computer");
    entryPoint.setIcon("?image=icons/nodes_wall.png");
    result.add(entryPoint);

    entryPoint = new ItemNode();
    entryPoint.setTitle("Queue");
    entryPoint.setPath("queue");
    entryPoint.setIcon("?image=icons/queue_wall.png");
    result.add(entryPoint);

    return result;
  }

  @Override
  public List<ItemNode> getReleaseNotes(final Account account,
      final PluginConfig pluginConf, final String version, final String url,
      final HttpServletRequest request) throws Exception {
    final ArrayList<ItemNode> changes = new ArrayList<ItemNode>();
    final JenkinsClient client = JenkinsClient.getInstance(account, pluginConf);
    final String jenkinsUrl = pluginConf.getUrl();
    final String jobPathPart = getJobPathPart(url, jenkinsUrl);
    final Job job = client.getJob(jobPathPart);
    final String jobBuildPart = getJobBuildPart(client, url, jenkinsUrl);
    final int jobBuildNumber = job.getBuild(jobBuildPart).number;

    final HashMap<URL, ItemNode> ticketsMap = new HashMap<URL, ItemNode>();
    final List<ItemNode> changesList = new ArrayList<ItemNode>();

    final ArtifactFingerprint remoteBuildFingerprint =
        client.getArtifactFromMD5(getApkMd5(request));

    if (job.builds.size() <= 0
        || remoteBuildFingerprint.original.number >= job.builds.get(0).number) {
      throw new Exception(
          "Nothing to upgrade: you already have the latest build");
    }
    for (final Build build : job.builds) {
      if (build.number > remoteBuildFingerprint.original.number
          && build.number <= jobBuildNumber) {
        for (final ChangeSetItem jobChange : client.getJobChanges(jobPathPart,
            build.number).items) {

          if (jobChange.issue != null) {
            final URL ticketUrl = jobChange.issue.linkUrl;
            if (ticketsMap.get(ticketUrl) == null) {
              ticketsMap.put(ticketUrl,
                  account.getPluginNodeForUrl(pluginConf, ticketUrl));
            }
          } else {
            changesList.add(jobChange.toAbstractNode(jenkinsUrl));
          }
        }
      }
    }

    if (!ticketsMap.isEmpty()) {
      changes.add(new HeaderNode("New features / Fixed bugs"));
      changes.addAll(ticketsMap.values());
    }

    if (!changesList.isEmpty()) {
      changes.add(new HeaderNode("Code changes"));
      changes.addAll(changesList);
    }
    return changes;
  }

  private String getApkMd5(final HttpServletRequest request) {
    final Matcher apkMd5Match =
        APK_MD5_PATTERN.matcher(request.getHeader(HttpHeaders.USER_AGENT));
    if (apkMd5Match.find()) {
      return apkMd5Match.group(1);
    } else {
      return null;
    }
  }

  private String getJobBuildPart(final JenkinsClient client,
      final String jobUrlString, final String jenkinsUrlString)
      throws Exception {
    final String jobPath = getJobPath(jobUrlString, jenkinsUrlString);

    final Matcher jobMatch = JOB_BUILD_PATTERN.matcher(jobPath);
    if (!jobMatch.find()) {
      throw new MalformedURLException("Cannot find job name in path " + jobPath);
    }

    return jobMatch.group(2);
  }

  private boolean isSameHost(final URL jobUrl, final URL jenkinsUrl) {
    final String jobHost = jobUrl.getHost();
    final String jenkinsHost = jenkinsUrl.getHost();
    return jobHost.equalsIgnoreCase(jenkinsHost);
  }

  private String getJobPathPart(final String jobUrlString,
      final String jenkinsUrlString) throws MalformedURLException {
    final String jobPath = getJobPath(jobUrlString, jenkinsUrlString);

    final Matcher jobMatch = JOB_PATTERN.matcher(jobPath);
    if (!jobMatch.find()) {
      throw new MalformedURLException("Cannot find job name in path " + jobPath);
    }

    return jobMatch.group(1);
  }

  private String getJobPath(final String jobUrlString,
      final String jenkinsUrlString) throws MalformedURLException {
    String jobPath;
    final URL jobUrl = new URL(jobUrlString);
    final URL jenkinsUrl = new URL(jenkinsUrlString);
    if (!isSameHost(jobUrl, jenkinsUrl)) {
      throw new MalformedURLException("URLs " + jobUrlString + " and Jenkins "
          + jenkinsUrlString + " have a different hostname");
    }

    jobPath = jobUrl.getPath();
    final String jenkinsPath = jenkinsUrl.getPath();
    if (!jobPath.startsWith(jenkinsPath)) {
      throw new MalformedURLException("Job path " + jobPath
          + " is not within Jenkins path " + jenkinsPath);
    }
    return jobPath;
  }

  @Override
  public ItemNode claim(final Account account, final PluginConfig pluginConf, final URL url) {
    return null;
  }

}
