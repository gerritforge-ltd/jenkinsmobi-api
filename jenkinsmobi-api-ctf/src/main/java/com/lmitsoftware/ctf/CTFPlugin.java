package com.lmitsoftware.ctf;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.commons.Config;
import mobi.jenkinsci.exceptions.TwoPhaseAuthenticationRequiredException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.lmitsoftware.ctf.model.poc.PocNodesProvider;
import com.lmitsoftware.ctf.model.poc.ProjectSprint;
import com.lmitsoftware.ctf.model.poc.ProjectsSprintPlanEntry;

public class CTFPlugin implements Plugin {

  private static final Logger log = Logger.getLogger(CTFPlugin.class);
  private String pluginName;
  private Injector injector;
  
  @Inject
  private Config config;

  @Override
  public AbstractNode processRequest(Account account, HttpServletRequest req,
      final PluginConfig pluginConf, String pluginName) throws Exception {
    this.pluginName = pluginName;
    log.debug("Query HTTP-" + req.getMethod() + ": " + req.getRequestURI());

    String imageName = req.getParameter("image");
    if (imageName != null && imageName.startsWith("sprint")) {
      return getSprintStatusImage(req, pluginConf, imageName);
    }
    
    ItemNode rootNode = new ItemNode();
    rootNode.addNode((ItemNode) getRootNode(req, pluginConf));
    return rootNode;
  }

  private ItemNode getRootNode(HttpServletRequest req,
      final PluginConfig pluginConf) throws URISyntaxException {
    PocNodesProvider.Factory nodesFactory = injector.getInstance(PocNodesProvider.Factory.class);
    ItemNode outNode = (ItemNode) nodesFactory.create(new UrlPath(req.getPathInfo()), pluginConf).get();
    return outNode;
  }

  private RawBinaryNode getSprintStatusImage(HttpServletRequest req, PluginConfig pluginConf, String imageName) throws Exception {
    String[] imageNameParts = imageName.split("\\.");
    ProjectSprint sprintDetails = (ProjectSprint) lookupNode(getRootNode(req, pluginConf), imageNameParts[0]);
    RawBinaryNode outImage = new RawBinaryNode();
    outImage.setData(sprintDetails.getImage());
    outImage.setHttpContentType(config.MIME_TYPES.get(imageNameParts[1]));
    outImage.setCacheable(false);
    return outImage;
  }

  private ItemNode lookupNode(ItemNode node, String nodeTitle) {
    String path = node.getPath();
    if(path != null && path.equalsIgnoreCase(nodeTitle)) {
      return node;
    } else {
      List<ItemNode> payload = node.getPayload();
      if(payload == null) {
        return null;
      }
      for (ItemNode childNode : payload) {
        ItemNode foundNode = lookupNode(childNode, nodeTitle);
        if(foundNode != null) {
          return foundNode;
        }
      }
    }
    
    return null;
  }

  @Override
  public void init() {
  }

  @Override
  public String getType() {
    return "TeamForge";
  }

  @Override
  public void configure(Properties configuration) {
    injector = Guice.createInjector(new CTFPluginModule());
  }

  @Override
  public List<ItemNode> getEntryPoints(PluginConfig pluginConf)
      throws Exception {
    return Collections.singletonList((ItemNode) new ProjectsSprintPlanEntry());
  }

  @Override
  public ItemNode claim(Account account, PluginConfig pluginConf, URL url)
      throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ItemNode> getReleaseNotes(Account account,
      PluginConfig pluginConf, String version, String url,
      HttpServletRequest request) throws Exception {
    return null;
  }

  @Override
  public String validateConfig(HttpServletRequest req, Account account,
      PluginConfig pluginConf) throws TwoPhaseAuthenticationRequiredException {
    return null;
  }
}
