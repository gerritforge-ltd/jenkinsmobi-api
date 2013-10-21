package com.lmitsoftware.ctf.model.poc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.google.inject.Inject;

public class ProjectsSprintPlan extends ProjectsSprintPlanEntry {
  private final SimpleDateFormat dateFmt = new SimpleDateFormat(
      "dd/MM/yyyy", Locale.UK);

  @Inject
  public ProjectsSprintPlan(ProjectSprint.Factory sprintFactory)
      throws ParseException {
    super();

    addNode(sprintFactory.create("Sprint 1", "sprint1",
        dateFmt.parse("15/01/2013"), dateFmt.parse("05/02/2013")));
    addNode(sprintFactory.create("Sprint 2", "sprint2",
        dateFmt.parse("06/02/2013"), dateFmt.parse("26/02/2013")));
    addNode(sprintFactory.create("Sprint 3", "sprint3",
        dateFmt.parse("27/02/2013"), dateFmt.parse("19/03/2013")));
    addNode(sprintFactory.create("Sprint 4", "sprint4",
        dateFmt.parse("20/03/2013"), dateFmt.parse("09/04/2013")));
    
    setViewTitle("Sprint Planning");
  }
}
