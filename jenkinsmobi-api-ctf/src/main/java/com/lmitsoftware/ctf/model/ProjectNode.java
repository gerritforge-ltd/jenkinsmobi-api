package com.lmitsoftware.ctf.model;

import mobi.jenkinsci.alm.Project;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.net.UrlPath;

import com.collabnet.ce.soap53.webservices.planning.ProjectSoapRow;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class ProjectNode extends ItemNode {

  public interface Factory {
    ProjectNode create(Project projectRow);
  }

  @Inject
  public ProjectNode(ProjectSprints.Factory sprintsFactory,
      UrlPath pathHelper, @Assisted Project projectRow) {
    super(Layout.LIST);
    setTitle(projectRow.title);
    setPath(projectRow.path);

    if (pathHelper.getComponents().size() > 1
        && pathHelper.getComponents().get(1)
            .equalsIgnoreCase(projectRow.path)) {
      addNode(sprintsFactory.create(projectRow.id));
      addNode(new ProjectReleases());
    }
  }
}
