package com.lmitsoftware.ctf.model.poc;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class ProjectSprint extends ItemNode {
  private static final HashMap<String, String[]> sprints =
      new HashMap<String, String[]>();
  private static final HashMap<String, String[]> boards =
      new HashMap<String, String[]>();
  private static final HashMap<String, String[]> releases =
      new HashMap<String, String[]>();
  private static final long DAYS_MSEC = 1000 * 3600 * 24;
  
  static {
    sprints.put("sprint1", new String[] {
        "planning.win_phone_frontend.discover_3_1.3_1_1_sprint_1",
        "planning.server.version_3_1.version_3_1_1,planning.blueprint_windows_phone_8.version_1_0.version_1_0_1"});
    boards.put("sprint1", new String[] {"", "50f3d1d21169e2c864006863"});
    releases.put("sprint1", new String[] {"rel1866,rel1867,rel1869", "rel1865"});

    sprints.put("sprint2", new String[] {
        "planning.win_phone_frontend.discover_3_1.3_1_2_sprint_2",
        "planning.server.version_3_1.version_3_1_2,planning.blueprint_windows_phone_8.version_1_0.version_1_0_2"});
    boards.put("sprint2", new String[] {"", "50f6885499ecc8cc5a002d5f"});
    releases.put("sprint2", new String[] {"rel1877,rel1878,rel1879,rel1881,rel1882,rel1883,rel1884", "rel1870"});

    sprints.put("sprint3", new String[] {
        "planning.win_phone_frontend.discover_3_1.3_1_3_sprint_3",
        "planning.server.version_3_1.version_3_1_3,planning.blueprint_windows_phone_8.version_1_0.version_1_0_3"});
    boards.put("sprint3", new String[] {"", "50f7f8a9f7175aaa64011650"});
    releases.put("sprint3", new String[] {"rel1891,rel1893,rel1917,rel1918,rel1919", "rel1888"});

    sprints.put("sprint4", new String[] {
        "planning.win_phone_frontend.discover_3_1.3_1_4_sprint_4",
        "planning.server.version_3_1.version_3_1_4,planning.blueprint_windows_phone_8.version_1_0.version_1_0_4"});
    boards.put("sprint4", new String[] {"", ""});
    releases.put("sprint4", new String[] {"", ""});
  }


  public interface Factory {
    ProjectSprint create(@Assisted("title") String title,
        @Assisted("path") String path, @Assisted("start") Date startDate,
        @Assisted("end") Date endDate);
  }

  private Date startDate;
  private Date endDate;

  @Inject
  public ProjectSprint(ProjectSprintDetailsCache sprintDetailsFactory,
      @Assisted("title") String title, @Assisted("path") String path,
      @Assisted("start") Date startDate, @Assisted("end") Date endDate) {
    super(Layout.LIST);
    setDescription(title);
    Date now = new Date();
    boolean isCurrentSprint =
        now.equals(startDate) || now.equals(endDate)
            || (now.after(startDate) && (now.before(endDate) || now.equals(endDate)));
    setModified("" + isCurrentSprint);
    setDescriptionColor(isCurrentSprint ? "#00a651" : "black");
    setTitle(String.format("%1$te %1$tb - %2$te %2$tb", startDate, endDate));
    this.startDate = startDate;
    this.endDate = endDate;
    setDescriptionAlign(Alignment.RIGHT);
    setViewTitle(title);
    setPath(path);

    String[] sprintPaths = sprints.get(path);
    String[] boardIds = boards.get(path);
    String[] releaseIds = releases.get(path);
    addNode(sprintDetailsFactory.create("WP8 Client", "projects.smhs",
        sprintPaths[0], boardIds[0], releaseIds[0]));
    addNode(sprintDetailsFactory.create("BluePrint / Backend", "projects.smhs",
        sprintPaths[1], boardIds[1], releaseIds[1]));
    addNode(new ProjectSprintLegend());
  }

  public InputStream getImage() throws Exception {
    List<ItemNode> sprintNodes = getPayload();
    ProjectSprintDetails clientSprint = (ProjectSprintDetails) sprintNodes.get(0);
    ProjectSprintDetails backendSprint = (ProjectSprintDetails) sprintNodes.get(1);
    int totDays = (int) ((endDate.getTime() - startDate.getTime())/DAYS_MSEC);
    int currDays = (int) ((System.currentTimeMillis() - startDate.getTime())/DAYS_MSEC);
    
    URL googleCharURL =
        new URL("http://chart.googleapis.com/chart?chma=0,100&"
            + "chxl=0:%7CTIME%7CCLIENT%7CBACKEND&" + "chf=bg,s,FFFFFF00&"
            + "chxr=0,0,3&" + "chxs=0,000000,18,1,lt,676767&" + "chxt=y&"
            + "chbh=a&" + "chs=576x230&" + "cht=bhs&"
            + "chco=80C65A,FFEAC0,EFEFEF&" + "chds=0,100,0,100,0,100&"
            + "chd=t:"
            + clientSprint.getDone()
            + ","
            + backendSprint.getDone()
            + ","
            + (currDays * 100) / totDays
            + "|"
            + clientSprint.getWip()
            + ","
            + backendSprint.getWip()
            + ",0|"
            + clientSprint.getOpen()
            + ","
            + backendSprint.getOpen()
            + ","
            + ((totDays - currDays) * 100) / totDays
            + "&"
            + "chm=t"
            + clientSprint.getDone()
            + "%25,000000,0,0,18|t"
            + clientSprint.getWip()
            + "%25,000000,1,0,18|t"
            + backendSprint.getDone()
            + "%25,000000,0,1,18|t"
            + backendSprint.getWip()
            + "%25,000000,1,1,18|t"
            + currDays
            + "d,000000,0,2,18|t" + (totDays - currDays) + "d,000000,2,2,18");
    return googleCharURL.openStream();
  }
}
