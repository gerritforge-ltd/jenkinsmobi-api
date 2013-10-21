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
package mobi.jenkinsci.alm.assembla;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.alm.assembla.objects.AssemblaItem;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;

public class AssemblaPlugin implements Plugin {
  private static final String ASSEMBLA_DOMAIN_NAME = "assembla.com";
  private static final String ASSEMBLA_TICKETS_URL_PART = "/tickets";
  private AssemblaClient client;

  @Override
  public AbstractNode processRequest(final Account account,
      final HttpServletRequest request, final PluginConfig pluginConf)
      throws IOException {
    final AssemblaClient client = client(pluginConf);

    if (request.getMethod().equalsIgnoreCase("GET")) {
      return getData(client, request);
    } else if (request.getMethod().equalsIgnoreCase("POST")) {
      return postData(client, request);
    } else {
      throw new IOException("Method " + request.getMethod()
          + " is NOT supported");
    }
  }

  private AssemblaClient client(final PluginConfig pluginConf) {
    if (client != null) {
      return client;
    }

    client =
        new AssemblaClient(pluginConf.getOptions().get("key"), pluginConf
            .getOptions().get("secret"), pluginConf.getUsername(),
            pluginConf.getPassword());
    return client;
  }

  public AbstractNode getData(final AssemblaClient client,
      final HttpServletRequest req) throws IOException {
    final ItemNode node =
        getNode(client, new UrlPath(req.getPathInfo()), false);
    node.setVersion(ItemNode.API_VERSION);
    node.setLeaf(true);
    return node;
  }

  public AbstractNode postData(final AssemblaClient client,
      final HttpServletRequest req) throws IOException {

    final AssemblaItem item =
        getItem(client, new UrlPath(req.getPathInfo()), false);
    final String postContentType = req.getContentType();
    final ServletInputStream postData = req.getInputStream();

    final AbstractNode node =
        item.post(postData, postContentType).serializeToJenkinsCloudObjects();
    node.setLeaf(true);
    return node;
  }

  private AssemblaItem getItem(final AssemblaClient client,
      final UrlPath requestPath, final boolean useAbsoluteNodePaths)
      throws IOException {
    final String nodeHead = requestPath.getHead();

    try {
      final EntryPoint entry = EntryPoint.valueOf(nodeHead.toUpperCase());
      switch (entry) {
        case SPACES:
          return client.spaces().getSubNode(requestPath.getTail(),
              useAbsoluteNodePaths);
        default:
          throw new IOException("Entry " + nodeHead + " not yet supported");
      }
    } catch (final IllegalArgumentException e) {
      throw new IOException("Entry " + nodeHead + " does not exist", e);
    }
  }

  private ItemNode getNode(final AssemblaClient client,
      final UrlPath requestPath, final boolean useAbsoluteNodePaths)
      throws IOException {
    final AssemblaItem item =
        getItem(client, requestPath, useAbsoluteNodePaths);
    if (useAbsoluteNodePaths) {
      item.applyPathPrefix("/" + getType() + "$");
    }
    return item.serializeToJenkinsCloudObjects();
  }

  @Override
  public void init() {
  }

  @Override
  public String getType() {
    return "Assembla";
  }

  @Override
  public List<ItemNode> getEntryPoints(final PluginConfig pluginConf)
      throws IOException {

    final List<ItemNode> result = new LinkedList<ItemNode>();

    for (final EntryPoint entryPoint : EntryPoint.values()) {
      final ItemNode entryNode = new ItemNode();
      entryNode.setTitle(entryPoint.getName());
      entryNode.setPath(entryPoint.getPath());
      entryNode.setIcon("?image=icons/ico-" + entryPoint.getPath() + ".png");
      result.add(entryNode);
    };

    return result;
  }

  @Override
  public ItemNode claim(final Account account, final PluginConfig pluginConf,
      final URL ticketUrl) throws IOException {
    final String host = ticketUrl.getHost().toLowerCase();
    if (!host.endsWith(ASSEMBLA_DOMAIN_NAME)) {
      return null;
    }

    final String ticketPath = ticketUrl.getPath();
    if (ticketPath.indexOf(ASSEMBLA_TICKETS_URL_PART) < 0) {
      return null;
    }

    final ItemNode node =
        getNode(client(pluginConf), new UrlPath(ticketPath), true);
    return node;
  }

  @Override
  public List<ItemNode> getReleaseNotes(final Account account,
      final PluginConfig pluginConf, final String version, final String url,
      final HttpServletRequest request) throws Exception {
    return null;
  }

  @Override
  public String validateConfig(final HttpServletRequest req,
      final Account account, final PluginConfig pluginConf)
      throws TwoPhaseAuthenticationRequiredException {
    return null;
  }
}
