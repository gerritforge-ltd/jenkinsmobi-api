package com.lmitsoftware.ctf.model.poc;

import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.model.ItemNode;

public class ArtifactCheckbox extends ItemNode {

  public ArtifactCheckbox(Item item) {
    setTitle(item.title);
    setIcon(item.isClosed() ? "?image=icons/checked.png"
        : "?image=icons/unchecked.png");
  }
}
