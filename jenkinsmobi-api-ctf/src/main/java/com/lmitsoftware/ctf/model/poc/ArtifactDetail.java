package com.lmitsoftware.ctf.model.poc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import com.google.common.base.Objects;

public class ArtifactDetail extends ItemNode {

  private static final HashMap<String, String> STATUS_COLOURS =
      new HashMap<String, String>();
  static {
    STATUS_COLOURS.put("dev in progress", "#F37C00");
    STATUS_COLOURS.put("ready to start", "#6A6A6A");
    STATUS_COLOURS.put("open", "#6A6A6A");
    STATUS_COLOURS.put("ready for devqa", "#222EB3");
    STATUS_COLOURS.put("ready for dev qa", "#222EB3");
    STATUS_COLOURS.put("incomplete", "#6A6A6A");
    STATUS_COLOURS.put("wip", "#F37C00");
    STATUS_COLOURS.put("devqa in progress", "#222EB3");
    STATUS_COLOURS.put("done", "#095305");
    STATUS_COLOURS.put("complete", "#095305");
  }
  
  private static final HashMap<String, String[]> ARTIFACT_ICONS =
      new HashMap<String, String[]>();
  private static final String[] DEFECTS_WITH_PRIORITY = new String[] {
      "defect_red.png", "defect_orange.png", "defect_green.png", "defect_grey.png" };
  static {
    ARTIFACT_ICONS.put("tracker.smhs_dev_qa_defects", DEFECTS_WITH_PRIORITY);
    ARTIFACT_ICONS.put("tracker.blueprint_dev_qa", DEFECTS_WITH_PRIORITY);
    ARTIFACT_ICONS.put("tracker.backend_external_defects",
        DEFECTS_WITH_PRIORITY);
    ARTIFACT_ICONS.put("tracker.windows_phone_fe_dev_tasks_0", DEFECTS_WITH_PRIORITY);
    ARTIFACT_ICONS.put("tracker.windows_phone_fe_external_defect", DEFECTS_WITH_PRIORITY);
    ARTIFACT_ICONS.put("tracker.development_tasks", new String[] {"task.png"});
    ARTIFACT_ICONS.put("tracker.bp_backlog", new String[] {"task.png"});
    ARTIFACT_ICONS.put("tracker.windows_phone_fe_dev_tasks_1",
        new String[] {"task.png"});
    ARTIFACT_ICONS.put("cards", new String[] {"card.png"});
  }
  
  public ArtifactDetail(Item artifact) {
    super(Layout.LIST);
    String artifactId = artifact.id;
    setTitle(artifact.title);
    setDescription(artifact.status);
    setDescriptionAlign(Alignment.RIGHT);
    setDescriptionColor(Objects.firstNonNull(
        STATUS_COLOURS.get(artifact.status.toLowerCase()), "black"));
    setViewTitle(artifactId);
    setPath(artifactId);

    addNode("Priority", artifact.priority);
    addNode("Status", artifact.status);
    addNode("Progress", (int) (artifact.getProgress() * 100) + "%");
    addNode(artifact.title);
    addNode(artifact.description);
    addNode("Submitter", artifact.submittedByFullname);
    addNode("Assigned to", artifact.assignedToFullname);
    addNode("Submit date", artifact.submittedDate);

    if (artifact.lastModifiedDate != null) {
      addNode("Last modified", artifact.lastModifiedDate);
      setTimestamp("" + artifact.lastModifiedDate.getTimeInMillis());
    }
    
    setIconForTracker(artifact);

    ArrayList<Item> subItems = artifact.subItems;
    if (subItems.size() > 0) {
      addNode(new HeaderNode(subItems.get(0).isCheckItem ? "Checklist"
          : "Tasks"));

      for (Item item : subItems) {
        if (item.isCheckItem) {
          addNode(new ArtifactCheckbox(item));
        } else {
          addNode(new ArtifactDetail(item));
        }
      }
    }
  }

  private void setIconForTracker(Item artifact) {
    if(artifact.trackerClass == null) {
      return;
    }
    
    String[] iconNames = ARTIFACT_ICONS.get(artifact.trackerClass);
    if(iconNames == null || iconNames.length <= 0) {
      return;
    }
    
    String iconName;
    if(artifact.priority <= iconNames.length) {
      iconName = iconNames[artifact.priority-1];
    } else {
      iconName = iconNames[0];
    }
    
    setIcon("?image=icons/" + iconName);
  }

  private void addNode(String string, Calendar submittedDate) {
    if (submittedDate != null) {
      addNode(new ArtifactField(string, submittedDate));
    }
  }

  public void addNode(String value) {
    if (value != null && value.trim().length() > 0) {
      addNode(new ArtifactField(value));
    }
  }

  private void addNode(String string, String status) {
    if (status != null && status.trim().length() > 0) {
      addNode(new ArtifactField(string, status));
    }
  }

  private void addNode(String string, int priority) {
    if (priority > 0) {
      addNode(new ArtifactField(string, priority));
    }
  }
}
