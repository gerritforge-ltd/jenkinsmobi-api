package com.lmitsoftware.ctf.model.poc;

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;

public class ProjectSprintLegend extends ItemNode {
  
  public ProjectSprintLegend() {
    setTitle("Legend");
    setIcon("?image=icons/legend.png");
    setIconAlign(Alignment.BOTTOM);
  }

}
