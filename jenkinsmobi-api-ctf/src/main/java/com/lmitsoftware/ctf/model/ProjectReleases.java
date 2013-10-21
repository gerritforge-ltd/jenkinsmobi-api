package com.lmitsoftware.ctf.model;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

public class ProjectReleases extends ItemNode {
  
  public ProjectReleases() {
    super(Layout.LIST);
    setTitle("Releases");
    setPath("releases");
  }

}
