package com.lmitsoftware.ctf.model.poc;

import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

public class ProjectsSprintPlanEntry extends ItemNode {

  public ProjectsSprintPlanEntry() {
    super(Layout.LIST);
    
    setTitle("Sprint plan");
    setPath("plan");
    setIcon("?image=icons/project.png");
  }
}
