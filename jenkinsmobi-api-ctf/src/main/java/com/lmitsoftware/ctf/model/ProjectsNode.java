package com.lmitsoftware.ctf.model;

import java.rmi.RemoteException;

import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Project;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import com.collabnet.ce.soap53.webservices.planning.ProjectSoapRow;
import com.google.inject.Inject;

public class ProjectsNode extends ItemNode {

  @Inject
  public ProjectsNode(ALMClient client, ProjectNode.Factory projectFactory)
      throws RemoteException {
    super(Layout.LIST);
    setPath("projects");
    setVersion(API_VERSION);

    Project[] projectRows = client.getProjects();
    for (Project projectSoapRow : projectRows) {
      addNode(projectFactory.create(projectSoapRow));
    }
  }
}
