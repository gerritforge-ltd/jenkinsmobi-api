package com.lmitsoftware.ctf.model;

import java.rmi.RemoteException;

import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Sprint;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import com.collabnet.ce.soap53.webservices.planning.IllegalArgumentFault;
import com.collabnet.ce.soap53.webservices.planning.InvalidSessionFault;
import com.collabnet.ce.soap53.webservices.planning.NoSuchObjectFault;
import com.collabnet.ce.soap53.webservices.planning.PermissionDeniedFault;
import com.collabnet.ce.soap53.webservices.planning.PlanningFolderSoapRow;
import com.collabnet.ce.soap53.webservices.planning.SystemFault;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lmitsoftware.ctf.CTFClient;

public class ProjectSprints extends ItemNode {
  
  public interface Factory {
    public ProjectSprints create(String projectId);
  }

  @Inject
  public ProjectSprints(CTFClient client, ProjectSprintFolder.Factory folderFactory, @Assisted String projectId) throws IllegalArgumentFault, NoSuchObjectFault, InvalidSessionFault, SystemFault, PermissionDeniedFault, RemoteException {
    super(Layout.LIST);
    setTitle("Sprint plan");
    setPath("sprints");
    
    Sprint[] folderList = client.getFolderList(projectId);
    for (Sprint planningFolderSoapRow : folderList) {
      addNode(folderFactory.create(planningFolderSoapRow, 0));
    }
  }
}
