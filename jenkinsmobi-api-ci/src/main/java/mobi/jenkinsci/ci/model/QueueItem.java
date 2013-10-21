// Copyright (C) 2013 GerritForge www.gerritforge.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package mobi.jenkinsci.ci.model;

import mobi.jenkinsci.model.Alignment;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;

import com.google.gson.annotations.SerializedName;

public class QueueItem extends JenkinsItem {

  @SerializedName("blocked")
  private boolean blocked;
  @SerializedName("stuck")
  private boolean stuck;
  @SerializedName("why")
  private String why;
  @SerializedName("task")
  private Task task;
  @SerializedName("id")
  private String id;

  public class Task {

    @SerializedName("name")
    public String name;

  }

  public boolean isBlocked() {
    return blocked;
  }

  public void setBlocked(boolean blocked) {
    this.blocked = blocked;
  }

  public boolean isStuck() {
    return stuck;
  }

  public void setStuck(boolean stuck) {
    this.stuck = stuck;
  }

  public String getWhy() {
    return why;
  }

  public void setWhy(String why) {
    this.why = why;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setTitle(task.name);
    result.setPath(path);

    ItemNode _task = new ItemNode("Task", task.name);

    ItemNode blocked =
        new ItemNode("Blocked", isBlocked() ? "Yes" : "No");
    blocked.setDescriptionColor(isBlocked() ? "#FF0000" : "#00FF00");
    blocked.setDescriptionAlign(Alignment.RIGHT);

    ItemNode stuck =
        new ItemNode("Stuck", isStuck() ? "Yes" : "No");
    blocked.setDescriptionColor(isStuck() ? "#FF0000" : "#00FF00");
    blocked.setDescriptionAlign(Alignment.RIGHT);

    ItemNode why =
        new ItemNode("Why in queue", getWhy());
    why.setDescriptionAlign(Alignment.BOTTOM);

    result.addNode(_task);
    result.addNode(blocked);
    result.addNode(stuck);
    result.addNode(why);
    
    result.setLeaf(true);

    return result;
  }
}
