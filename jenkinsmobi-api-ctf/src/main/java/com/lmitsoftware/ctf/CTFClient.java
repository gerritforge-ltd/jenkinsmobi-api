package com.lmitsoftware.ctf;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.rpc.ServiceException;

import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.alm.Project;
import mobi.jenkinsci.alm.Sprint;
import mobi.jenkinsci.alm.SprintSummary;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.log4j.Logger;

import com.collabnet.ce.soap53.webservices.planning.Artifact2SoapList;
import com.collabnet.ce.soap53.webservices.planning.Artifact2SoapRow;
import com.collabnet.ce.soap53.webservices.planning.ArtifactsInPlanningFolderSoapRow;
import com.collabnet.ce.soap53.webservices.planning.CollabNetSoap;
import com.collabnet.ce.soap53.webservices.planning.CollabNetSoapServiceLocator;
import com.collabnet.ce.soap53.webservices.planning.FrsAppSoap;
import com.collabnet.ce.soap53.webservices.planning.FrsAppSoapServiceLocator;
import com.collabnet.ce.soap53.webservices.planning.InvalidSessionFault;
import com.collabnet.ce.soap53.webservices.planning.NoSuchObjectFault;
import com.collabnet.ce.soap53.webservices.planning.PermissionDeniedFault;
import com.collabnet.ce.soap53.webservices.planning.PlanningAppSoap;
import com.collabnet.ce.soap53.webservices.planning.PlanningAppSoapServiceLocator;
import com.collabnet.ce.soap53.webservices.planning.PlanningFolderSoapRow;
import com.collabnet.ce.soap53.webservices.planning.PlanningFolderSummarySoapDO;
import com.collabnet.ce.soap53.webservices.planning.ProjectSoapRow;
import com.collabnet.ce.soap53.webservices.planning.SystemFault;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CTFClient implements ALMClient {

  private static Logger log = Logger.getLogger(CTFClient.class);

  private CollabNetSoap main;
  private PlanningAppSoap planning;
  private FrsAppSoap release;

  private String sessionid;
  private String url;
  private String username;


  @Inject
  public CTFClient(PluginConfig pluginConf) throws ServiceException {

    this.url = pluginConf.getUrl();
    this.username = pluginConf.getUsername();

    if (!url.endsWith("/")) url = url + "/";

    String mainUrl = url + "ce-soap50/services/CollabNet";
    String planningUrl = url + "ce-soap50/services/PlanningApp";
    String releaseUrl = url + "ce-soap50/services/FrsApp";
    log.info("Teamforge endpoints: \n" + "main=" + mainUrl + "\n" + "planning="
        + planningUrl);

    CollabNetSoapServiceLocator mainLocator = new CollabNetSoapServiceLocator();
    mainLocator.setCollabNetEndpointAddress(mainUrl);
    main = mainLocator.getCollabNet();

    PlanningAppSoapServiceLocator planningServiceLocator =
        new PlanningAppSoapServiceLocator();
    planningServiceLocator.setPlanningAppEndpointAddress(planningUrl);
    planning = planningServiceLocator.getPlanningApp();

    FrsAppSoapServiceLocator releaseServiceLocator =
        new FrsAppSoapServiceLocator();
    releaseServiceLocator.setFrsAppEndpointAddress(releaseUrl);
    release = releaseServiceLocator.getFrsApp();

    try {
      String version = main.getApiVersion();
      log.info("Teamforge remote version: " + version);

      login(username, pluginConf.getPassword());
      log.info("Signed in as " + username);

    } catch (RemoteException ex) {
      log.warn("Unable to retrieve API version from url " + url, ex);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.lmitsoftware.ctf.ALMClient#login(java.lang.String,
   * java.lang.String)
   */
  @Override
  public void login(String username, String password) throws RemoteException {
    try {
      logout();
      this.username = username;
      this.sessionid = main.login(username, password);
    } catch (RemoteException e) {
      sessionid = null;
      throw e;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.lmitsoftware.ctf.ALMClient#logout()
   */
  @Override
  public void logout() throws RemoteException {
    try {
      if (this.sessionid != null) {
        main.logoff(username, sessionid);
      }
    } finally {
      this.sessionid = null;
      this.username = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.lmitsoftware.ctf.ALMClient#getProjects()
   */
  @Override
  public Project[] getProjects() throws RemoteException {
    ProjectSoapRow[] projectSoapRows =
        main.getProjectList(sessionid).getDataRows();
    Project[] projects = new Project[projectSoapRows.length];
    for (int i = 0; i < projectSoapRows.length; i++) {
      ProjectSoapRow projectRow = projectSoapRows[i];
      projects[i] =
          new Project(projectRow.getId(), projectRow.getTitle(),
              projectRow.getPath());
    }
    return projects;
  }

  @Override
  public SprintSummary getSprintSummary(String parentId) throws RemoteException {
    try {
      PlanningFolderSummarySoapDO folderSummary =
          planning.getPlanningFolderSummary(sessionid, parentId);
      return new SprintSummary(folderSummary.getTotalOpen(),
          folderSummary.getTotalClosed());
    } catch (Exception e) {
      throw new RemoteException("Unable to get folder summary for " + parentId,
          e);
    }
  }

  @Override
  public Sprint[] getFolderList(String projectId) throws RemoteException {
    PlanningFolderSoapRow[] folders =
        planning.getPlanningFolderList(sessionid, projectId, false)
            .getDataRows();
    Sprint[] sprints = new Sprint[folders.length];
    for (int i = 0; i < folders.length; i++) {
      PlanningFolderSoapRow folder = folders[i];
      sprints[i] =
          new Sprint(folder.getId(), folder.getTitle(), folder.getPath());
    }

    return sprints;
  }

  @Override
  public String getProjectId(String projectPath) throws RemoteException {
    ProjectSoapRow[] projects = main.getProjectList(sessionid).getDataRows();
    for (ProjectSoapRow projectSoapRow : projects) {
      if (projectSoapRow.getPath().equalsIgnoreCase(projectPath)) {
        return projectSoapRow.getId();
      }
    }
    throw new RemoteException("Project with path " + projectPath
        + " was not found");
  }

  @Override
  public String getFolderId(String projectId, String folderPath)
      throws RemoteException {
    PlanningFolderSoapRow[] folders =
        planning.getPlanningFolderList(sessionid, projectId, true)
            .getDataRows();
    for (PlanningFolderSoapRow planningFolderSoapRow : folders) {
      String path = planningFolderSoapRow.getPath();
      if (path.equalsIgnoreCase(folderPath)) {
        return planningFolderSoapRow.getId();
      }
    }
    throw new RemoteException("Folder with path " + folderPath
        + " was not found in project " + projectId);
  }

  @Override
  public Item[] getFolderArtifacts(String folderId) throws RemoteException {
    ArtifactsInPlanningFolderSoapRow[] artifacts =
        planning.getArtifactListInPlanningFolder(sessionid, folderId, null,
            true).getDataRows();
    Item[] items = new Item[artifacts.length];
    for (int i = 0; i < artifacts.length; i++) {
      ArtifactsInPlanningFolderSoapRow artifact = artifacts[i];
      items[i] =
          new Item(artifact.getId(), artifact.getStatus(), artifact.getTitle(),
              artifact.getPriority(), artifact.getDescription(),
              artifact.getSubmittedByFullname(),
              artifact.getAssignedToFullname(), artifact.getSubmittedDate(),
              artifact.getLastModifiedDate(), artifact.getFolderPathString());
    }
    return items;
  }

  @Override
  public Sprint[] getSubSprintPlan(String sprintId) throws RemoteException {
    return getFolderList(sprintId);
  }

  public Collection<Item> getReportedOrFixedArtifacts(String relId)
      throws NoSuchObjectFault, InvalidSessionFault, SystemFault,
      PermissionDeniedFault, RemoteException {
    HashMap<String, Item> outItems = new HashMap<String, Item>();

    if (relId != null && relId.trim().length() > 0) {
      getArtifactsById(
          release.getArtifactListReportedInRelease(sessionid, relId)
              .getDataRows(), outItems);
      getArtifactsById(
          release.getArtifactListResolvedInRelease(sessionid, relId)
              .getDataRows(), outItems);
    }

    return outItems.values();
  }

  private void getArtifactsById(Artifact2SoapRow[] artifacts,
      HashMap<String, Item> outItems) throws RemoteException,
      NoSuchObjectFault, InvalidSessionFault, SystemFault,
      PermissionDeniedFault {
    for (Artifact2SoapRow artifact : artifacts) {
      String artifactId = artifact.getId();
      Item outItem = outItems.get(artifactId);
      if (outItem == null) {
        
        outItem =
            new Item(artifact.getId(), artifact.getStatus(),
                artifact.getTitle(), artifact.getPriority(),
                artifact.getDescription(), artifact.getSubmittedByFullname(),
                artifact.getAssignedToFullname(), artifact.getSubmittedDate(),
                artifact.getLastModifiedDate(), artifact.getFolderPathString());
        outItems.put(artifactId, outItem);
      }
    }
  }
}
