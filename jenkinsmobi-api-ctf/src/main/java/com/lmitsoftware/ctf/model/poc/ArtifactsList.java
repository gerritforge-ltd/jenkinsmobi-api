package com.lmitsoftware.ctf.model.poc;

import java.util.ArrayList;

import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.collabnet.ce.soap53.webservices.planning.ArtifactsInPlanningFolderSoapRow;

public class ArtifactsList extends ItemNode {

  public ArtifactsList(String status,
      ArrayList<Item> artifacts) {
    super(Layout.LIST);
    setTitle(status + " (" + artifacts.size() + ")"); 
    setViewTitle(status);
    setPath(UrlPath.normalizePath(status));

    for (Item artifact : artifacts) {
      addNode(new ArtifactDetail(artifact));
    }
  }

}
