package com.lmitsoftware.ctf.model.poc;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.HeaderNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lmitsoftware.ctf.CTFClient;

public class ProjectSprintDetails extends ItemNode {
  private static final Pattern REQ_PATTERN = Pattern
      .compile("(CSHS-[0-9]+)(\\.BP|\\.BE)?");
  private static final Pattern ARTF_PATTERN = Pattern.compile("(artf[0-9]+)");

  public interface Factory {
    ProjectSprintDetails create(@Assisted("title") String title,
        @Assisted("project") String projectPath,
        @Assisted("folder") String folderPath,
        @Assisted("boardId") String boardId,
        @Assisted("releaseId") String releaseId);
  }

  private int notStarted;
  private int wip;
  private int completed;

  @Inject
  public ProjectSprintDetails(CTFClient alm, @Assisted("title") String title,
      @Assisted("project") String projectPath,
      @Assisted("folder") String folderPath,
      @Assisted("boardId") String boardId,
      @Assisted("releaseId") String releaseId) throws RemoteException {
    super(Layout.LIST);

    ArrayList<Item> totalArtifacts =
        getCtfArtifacts(alm, projectPath, folderPath, releaseId);

    notStarted = 0;
    wip = 0;
    completed = 0;

    for (Item artifact : totalArtifacts) {
      if (artifact.isClosed()) {
        completed += 100;
      } else if (artifact.isPending()) {
        double progress = artifact.getProgress();
        wip += (progress * 100);
        notStarted += ((1.0F - progress) * 100);
      } else {
        notStarted += 100;
      }
    }

    int total = notStarted + wip + completed;
    if (total != 0) {
      notStarted = (notStarted * 100) / total;
      wip = (wip * 100) / total;
      completed = (completed * 100) / total;
    }

    setTitle(title);
    setViewTitle(title);
    setPath(UrlPath.normalizePath(title));
    String googleGraphURL = getGoogleGraphURL(completed, wip, notStarted);
    if (googleGraphURL != null) {
      setIcon(googleGraphURL);
      setIconAlign(Alignment.BOTTOM);
    }

    HashMap<String, ArrayList<Item>> artifactsByStatus =
        new HashMap<String, ArrayList<Item>>();

    ArrayList<Item> priorityArtifacts = new ArrayList<Item>();

    for (Item artifact : totalArtifacts) {
      String status = artifact.status;
      ArrayList<Item> artifactList =
          Objects.firstNonNull(artifactsByStatus.get(status),
              new ArrayList<Item>());
      artifactList.add(artifact);
      artifactsByStatus.put(status, artifactList);

      if (artifact.priority <= 1 && !artifact.isClosed()) {
        priorityArtifacts.add(artifact);
      }
    }

    if (priorityArtifacts.size() > 0) {
      ArtifactsList urgentItemsNode =
          new ArtifactsList("P1 Open items", priorityArtifacts);
      urgentItemsNode.setIcon("?image=icons/urgent.png");
      addNode(urgentItemsNode);
      addNode(new HeaderNode("Items per status"));
    }

    for (Entry<String, ArrayList<Item>> entry : artifactsByStatus.entrySet()) {
      addNode(new ArtifactsList(entry.getKey(), entry.getValue()));
    }
  }

  private ArrayList<Item> getCtfArtifacts(CTFClient alm, String projectPath,
      String folderPath, String releaseId) throws RemoteException {
    ArrayList<Item> outItems = new ArrayList<Item>();
    String[] folderPaths = folderPath.split(",");
    String projectId = alm.getProjectId(projectPath);

    for (String path : folderPaths) {
      String folderId = alm.getFolderId(projectId, projectPath + "/" + path);
      Item[] artifacts = alm.getFolderArtifacts(folderId);
      outItems.addAll(Arrays.asList(artifacts));
    }

    String[] releaseIds = releaseId.split(",");
    for (String relId : releaseIds) {
      outItems.addAll(alm.getReportedOrFixedArtifacts(relId));
    }

    return outItems;
  }

  private String getGoogleGraphURL(int completed, int wip, int notStarted) {
    int tot = completed + wip + notStarted;
    if (tot == 0) {
      return null;
    }

    String completedValue = getPercentValue(completed, tot, 1);
    String wipValue = getPercentValue(wip, tot, 10);
    String googleGraphApi =
        "http://chart.googleapis.com/chart" + "?chxl=0:%7C+%7C1:%7C+"
            + "&chxr=0,0,0"
            + "&chxs=0,676767,11.5,-1,_,676767%7C1,676767,11.5,-1,t,676767"
            + "&chxt=x,y" + "&chbh=a" + "&chs=400x100" + "&cht=bhs"
            + "&chco=80C65A,F9C785,F4F4F4" + "&chds=0," + tot + ",0," + tot
            + ",0," + tot + "&chd=t:" + completed + "%7C" + wip + "%7C"
            + notStarted + "&chma=22,20,20,30%7C7,51" + "&chm=t"
            + completedValue + ",000000,0,0:0,15%7Ct" + wipValue
            + ",000000,1,-1,15" + "&chf=bg,s,FFFFFF00";
    return googleGraphApi;
  }

  private String getPercentValue(int vaue, int tot, int minValue) {
    int valuePerc = (vaue * 100) / tot;
    if (valuePerc < minValue) {
      return "";
    } else {
      return valuePerc + "%25";
    }
  }

  public int getDone() {
    return completed;
  }

  public int getWip() {
    return wip;
  }

  public int getOpen() {
    return notStarted;
  }
}
