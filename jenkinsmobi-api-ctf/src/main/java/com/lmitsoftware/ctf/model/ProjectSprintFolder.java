package com.lmitsoftware.ctf.model;

import java.rmi.RemoteException;

import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Sprint;
import mobi.jenkinsci.alm.SprintSummary;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.collabnet.ce.soap53.webservices.planning.PlanningFolderSoapRow;
import com.collabnet.ce.soap53.webservices.planning.PlanningFolderSummarySoapDO;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lmitsoftware.ctf.CTFClient;

public class ProjectSprintFolder extends ItemNode {

  public interface Factory {
    ProjectSprintFolder create(Sprint planningFolderSoapRow,
        int folderLevel);
  }

  @Inject
  public ProjectSprintFolder(CTFClient client, UrlPath path,
      @Assisted Sprint planningFolderSoapRow,
      @Assisted int folderLevel) throws RemoteException {
    super(Layout.LIST);

    setTitle(planningFolderSoapRow.title
        + "\n"
        + getFolderSummary(client.getSprintSummary(planningFolderSoapRow.id)));
    String folderPath = planningFolderSoapRow.path;
    setPath(folderPath.substring(folderPath.lastIndexOf('/') + 1));

    if (path.getComponents().size() < folderLevel + 3) {
      return;
    }

    Sprint[] subFolders =
        client.getFolderList(planningFolderSoapRow.id);
    for (Sprint subFolder : subFolders) {
      addNode(new ProjectSprintFolder(client, path, subFolder, folderLevel + 1));
    }
  }

  private String getFolderSummary(SprintSummary folderSummary) {
    int closed = folderSummary.totalClosed;
    int open = folderSummary.totalOpen;

    if (open + closed <= 0) {
      return "";
    } else {
      return "Progress: " + ((closed * 100) / (open + closed)) + "%";
    }
  }
}
