package com.lmitsoftware.ctf.model.poc;

import java.util.Calendar;

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;

public class ArtifactField extends ItemNode {

  private static final String[] PRIORITY_STR = {"None", "1 - Highest",
      "2 - High", "3 - Medium", "4 - Low", "5 - Lowest"};
  private static final String[] PRIORITY_COLOUR = {"#DDDDDD", "#ee1c24",
      "#f26522", "#00a651", "#7d7d7d", "#AAAAAA"};


  public ArtifactField(String title) {
    setTitle(title);
  }

  public ArtifactField(String label, String value) {
    setTitle(label);
    setDescription(value);
    setDescriptionAlign(Alignment.RIGHT);
  }

  public ArtifactField(String label, Calendar date) {
    setTitle(label);
    setDescription(String.format("%1$te %1$tb %1$tY ", date));
    setDescriptionAlign(Alignment.RIGHT);
  }

  public ArtifactField(String label, int priority) {
    setTitle(label);
    setDescription(PRIORITY_STR[priority]);
    setDescriptionColor(PRIORITY_COLOUR[priority]);
    setDescriptionAlign(Alignment.RIGHT);
  }
}
