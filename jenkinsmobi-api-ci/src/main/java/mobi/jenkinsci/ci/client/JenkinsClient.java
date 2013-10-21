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
package mobi.jenkinsci.ci.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.ci.client.sso.AssemblaSsoHandler;
import mobi.jenkinsci.ci.client.sso.GitHubSsoHandler;
import mobi.jenkinsci.ci.client.sso.GoogleSsoHandler;
import mobi.jenkinsci.ci.model.Build;
import mobi.jenkinsci.ci.model.ChangeSet;
import mobi.jenkinsci.ci.model.ChangeSetItem;
import mobi.jenkinsci.ci.model.ChangeSetItem.Issue;
import mobi.jenkinsci.ci.model.ComputerList;
import mobi.jenkinsci.ci.model.FailedSuite;
import mobi.jenkinsci.ci.model.FailedTest;
import mobi.jenkinsci.ci.model.FailedTestsList;
import mobi.jenkinsci.ci.model.JenkinsItem;
import mobi.jenkinsci.ci.model.Job;
import mobi.jenkinsci.ci.model.Queue;
import mobi.jenkinsci.ci.model.View;
import mobi.jenkinsci.ci.model.ViewList;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Constants;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ResetNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

public class JenkinsClient {
  public static final String VIEWS_ALL = "_all";
  private static final Logger LOG = Logger.getLogger(JenkinsClient.class);
  private static final HashMap<PluginConfig, JenkinsClient> clients =
      new HashMap<PluginConfig, JenkinsClient>();
  private static final int QUERY_DEPTH = 1;
  private static final String PRETTY_JSON = Boolean
      .getBoolean("JENKINS_CLOUD_JSON_PRETTY") ? "&pretty=true" : "";
  private static final String QUERY_STRING = "?depth=" + QUERY_DEPTH
      + PRETTY_JSON;
  private static final HashSet<String> PROPAGATED_HEADERS_WHITELIST =
      new HashSet<String>();
  static {
    PROPAGATED_HEADERS_WHITELIST.add(Constants.X_AUTH_OTP_HEADER.toLowerCase());
    PROPAGATED_HEADERS_WHITELIST.add(HttpHeaders.USER_AGENT.toLowerCase());

    GitHubSsoHandler.init();
    GoogleSsoHandler.init();
    AssemblaSsoHandler.init();
  }

  public final JenkinsConfig config;
  public final JenkinsHttpClient http;
  public final Account account;

  public JenkinsClient(final Account account, final JenkinsConfig confObject)
      throws MalformedURLException {
    this.config = confObject;
    this.http = new JenkinsHttpClient(confObject);
    this.account = account;
  }

  public static JenkinsClient getInstance(final Account account,
      final PluginConfig pluginConf) throws MalformedURLException {
    JenkinsClient client = clients.get(pluginConf);
    if (client == null) {
      client = new JenkinsClient(account, new JenkinsConfig(pluginConf));
      clients.put(pluginConf, client);
    }

    return client;
  }

  public <T> T load(final String url, final String queryString,
      final Class<T> returnType, final HashMap<String, String> extraHeaders)
      throws IOException {
    final String query = url + "/api/json" + queryString;

    LOG.info("Request to Jenkins: '" + query + "'");

    final Map<String, String> headers = new HashMap<String, String>();
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    final HttpGet get = new HttpGet(query);
    for (final Entry<String, String> header : headers.entrySet()) {
      get.addHeader(header.getKey(), header.getValue());
    }
    try {
      final InputStream result = http.getInputStream(get);
      final InputStreamReader jsonReader =
          new InputStreamReader(result, "UTF-8");
      try {
        final T outObj = new Gson().fromJson(jsonReader, returnType);
        if (outObj instanceof JenkinsItem) {
          ((JenkinsItem) outObj).init(this);
        }
        return outObj;
      } finally {
        jsonReader.close();
      }
    } finally {
      get.releaseConnection();
    }
  }

  public Document loadPage(final String url,
      final HashMap<String, String> extraHeaders) throws IOException {
    LOG.info("Request to Jenkins Page: '" + url + "'");

    final Map<String, String> headers = new HashMap<String, String>();
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    final HttpGet get = new HttpGet(url);
    for (final Entry<String, String> header : headers.entrySet()) {
      get.addHeader(header.getKey(), header.getValue());
    }
    try {
      final InputStream result = http.getInputStream(get);
      return Jsoup.parse(result, "UTF-8", url);
    } finally {
      get.releaseConnection();
    }
  }


  public ViewList getViewList() throws IOException {
    return getViewList(null);
  }

  public ViewList getViewList(final HttpServletRequest req) throws IOException {
    final String query = QUERY_STRING;
    final ViewList viewList =
        load(config.getUrl(), query, ViewList.class, getHeaders(req));
    viewList.path = ("views");
    return viewList;
  }

  private HashMap<String, String> getHeaders(final HttpServletRequest req) {
    final HashMap<String, String> propagatedHeader =
        new HashMap<String, String>();
    if (req != null) {
      final Enumeration<String> headerNames = req.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        final String headerName = headerNames.nextElement();
        if (PROPAGATED_HEADERS_WHITELIST.contains(headerName.toLowerCase())) {
          propagatedHeader.put(headerName, req.getHeader(headerName));
        }
      }
    }
    return propagatedHeader;
  }

  public Job getJob(final String jobPath) throws IOException {
    final String query = QUERY_STRING;
    final Job job =
        load(config.getUrl() + "/job/" + jobPath, query, Job.class, null);
    job.path = jobPath;
    return job;
  }

  public static String urlEncode(final String url) {
    // Seems that Jenkins doesn't like spaces URL-encoded as '+'
    try {
      return URLEncoder.encode(url, "UTF-8").replaceAll("[+]", "%20");
    } catch (final UnsupportedEncodingException e) {
      return url;
    }
  }

  public ComputerList getComputerList() throws IOException {
    final ComputerList computerList =
        load(config.getUrl() + "/computer", "", ComputerList.class, null);
    computerList.path = ("computer");
    return computerList;
  }

  public Queue getQueue() throws IOException {
    final Queue queue = load(config.getUrl() + "/queue", "", Queue.class, null);
    queue.path = ("queue");
    return queue;
  }

  public FailedTestsList getFailedTestsList() throws IOException {
    final String query = "?tree=suites[name,cases[status,name,failedSince]]";
    final FailedTestsList list =
        load(config.getUrl(), query, FailedTestsList.class, null);
    list.path = ("failedTest");
    filterForTestStatus(list, "FAILED");
    return list;
  }

  private <T> void filterForTestStatus(final T result, final String status) {
    final FailedTestsList list = (FailedTestsList) result;
    final List<FailedSuite> suiteToRemoveList = new LinkedList<FailedSuite>();
    for (final FailedSuite suite : list.getSuites()) {
      final List<FailedTest> toRemove = new LinkedList<FailedTest>();
      for (final FailedTest test : suite.getCases()) {
        if (!test.getStatus().equalsIgnoreCase(status)) {
          toRemove.add(test);
        }
      }
      // remove
      for (final FailedTest testToRemove : toRemove) {
        suite.getCases().remove(testToRemove);
      }

      if (suite.getCases().size() == 0) {
        suiteToRemoveList.add(suite);
      }
    }

    for (final FailedSuite suiteToRemove : suiteToRemoveList) {
      list.getSuites().remove(suiteToRemove);
    }
  }

  public JenkinsItem getVewDetails(final String viewName) throws IOException {
    final String query = QUERY_STRING;
    final JenkinsItem view =
        load(
            config.getUrl()
                + (viewName.equals(VIEWS_ALL) ? "" : "/view/"
                    + urlEncode(viewName)), query, View.class, null);
    view.path = (viewName);
    return view;
  }

  public String getCommandUrl(String command) {
    if (command.startsWith("http")) {
      return command;
    }

    String baseUrl = config.getUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    if (command.startsWith("/")) {
      command = command.substring(1);
    }
    return baseUrl + command;
  }

  public AbstractNode execute(final UrlPath reqPath, final String command)
      throws IOException {
    final List<String> pathComponents = reqPath.getComponents();
    return execute("job/" + pathComponents.get(pathComponents.size() - 1) + "/"
        + command + "?delay=0");
  }

  public AbstractNode execute(final String command) throws IOException {
    final HttpPost post = new HttpPost(getCommandUrl(command));
    try {
      final HttpResponse response = http.execute(post);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpURLConnection.HTTP_OK
          || statusCode == HttpURLConnection.HTTP_MOVED_PERM
          || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
        return new ResetNode();
      } else {
        throw new IOException("Command " + command
            + " *FAILED* with HTTP Status " + response.getStatusLine());
      }
    } finally {
      post.releaseConnection();
    }
  }

  public ChangeSet getJobChanges(final String jobPath, final int jobBuildNumber)
      throws IOException {
    final Document changePage =
        loadPage(config.getUrl() + "/job/" + jobPath + "/" + jobBuildNumber
            + "/changes", null);
    final Element changesList = changePage.select("table[class=pane]").first();
    final HashMap<String, Issue> issues = getIssuesFromTable(changesList);

    final ChangeSet changeSet =
        load(config.getUrl() + "/job/" + jobPath + "/" + jobBuildNumber,
            QUERY_STRING, Build.class, null).changeSet;
    for (final Iterator<ChangeSetItem> iterator = changeSet.items.iterator(); iterator
        .hasNext();) {
      final ChangeSetItem changeItem = iterator.next();
      changeItem.issue = issues.get(changeItem.getUniqueId());
    }

    return changeSet;
  }

  private HashMap<String, Issue> getIssuesFromTable(final Element changesTable) {
    final HashMap<String, Issue> issues =
        new HashMap<String, ChangeSetItem.Issue>();
    if (changesTable == null) {
      return issues;
    }

    if (changesTable.children().size() <= 0) {
      LOG.warn("Cannot find changes TBODY");
      return issues;
    }

    final Element tbody = changesTable.child(0);
    final Elements rows = tbody.children();
    for (final Element row : rows) {
      final String commitId = getCommitIdFromRow(row);
      Issue issue;
      try {
        issue = getIssueFromRow(row);
        if (issue != null) {
          issues.put(commitId, issue);
        }
      } catch (final MalformedURLException e) {
        LOG.warn("Invalid issue URL for row " + row.toString() + ": skipping",
            e);
      }
    }

    return issues;
  }

  private Issue getIssueFromRow(final Element row) throws MalformedURLException {
    final Element fullChangeMessage =
        row.select("div[class=changeset-message]").first();
    if (fullChangeMessage == null) {
      return null;
    }

    final Element issueLink =
        fullChangeMessage.select("pre").first().select("a").first();
    if (issueLink == null) {
      return null;
    } else {
      final Element issueIcon = issueLink.select("img").first();
      return new Issue(getUrl(issueLink, "href"), issueLink.attr("tooltip"),
          getUrl(issueIcon, "src"));
    }
  }

  private URL getUrl(final Element issueLink, final String attr)
      throws MalformedURLException {
    if (issueLink == null) {
      return null;
    }

    final String linkUrl = issueLink.attr(attr);
    if (linkUrl == null) {
      return null;
    }

    if (linkUrl.startsWith("http")) {
      return new URL(linkUrl);
    } else {
      final URL baseUrl = new URL(issueLink.baseUri());
      return new URL(baseUrl, linkUrl);
    }
  }

  private String getCommitIdFromRow(final Element row) {
    final Element fullChangeDesc =
        row.select("div[class=changeset-message]").first();
    if (fullChangeDesc == null) {
      return null;
    }
    final Element message = fullChangeDesc.select("b").first();
    final String messageText = message.childNode(0).toString();
    final Matcher commitMatch =
        Pattern.compile("Commit ([^ ]+)").matcher(messageText);
    if (commitMatch.find()) {
      return commitMatch.group(1);
    } else {
      return null;
    }
  }

  public ArtifactFingerprint getArtifactFromMD5(final String md5Fingerprint)
      throws Exception {
    return load(config.getUrl() + "/fingerprint/" + md5Fingerprint,
        QUERY_STRING, ArtifactFingerprint.class, null);
  }
}
