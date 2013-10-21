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
package mobi.jenkinsci.alm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Item {
  public final String id;
  public final String status;
  public final String title;
  public final int priority;
  public final String description;
  public final String submittedByFullname;
  public final String assignedToFullname;
  public final Calendar submittedDate;
  public final Calendar lastModifiedDate;
  public final ArrayList<Item> subItems = new ArrayList<Item>();
  public final boolean isCheckItem;
  public final String trackerClass;

  public Item(String id, String status, String title, int priority,
      String description, String submittedByFullName,
      String assignedToFullname, Calendar submittedDate,
      Calendar lastModifiedDate, String trackerClass) {
    this.id = id;
    this.status = status;
    this.title = title;
    this.priority = priority;
    this.description = description;
    this.submittedByFullname = submittedByFullName;
    this.assignedToFullname = assignedToFullname;
    this.submittedDate = submittedDate;
    this.lastModifiedDate = lastModifiedDate;
    this.isCheckItem = false;
    this.trackerClass = trackerClass;
  }
  
  public Item(String id, String checkItem, boolean completed) {
    this.id = id;
    this.status = completed ? "complete":"incomplete";
    this.title = checkItem;
    this.priority = -1;
    this.submittedByFullname = null;
    this.description = null;
    this.assignedToFullname = null;
    this.submittedDate = null;
    this.lastModifiedDate = null;
    this.isCheckItem = true;
    this.trackerClass = null;
  }

  public void addSubItem(Item item) {
    subItems.add(item);
  }

  public void addSubItems(List<Item> itemList) {
    subItems.addAll(itemList);
  }

  public boolean isClosed() {
    if(status.equalsIgnoreCase("closed")
        || status.equalsIgnoreCase("cancelled")
        || status.equalsIgnoreCase("complete")
        || status.equalsIgnoreCase("rejected")
        || status.toLowerCase().indexOf("fixed") >= 0 
        || status.equalsIgnoreCase("done")
        || status.toLowerCase().indexOf("passed") >= 0) {
      return true;
    }
    
    if (subItems.size() > 0) {
      return getProgress() == 1.0;
    } else {
      return false;
    }
  }

  public boolean isPending() {
    return !isOpen() && !isClosed();
  }

  private boolean isOpen() {
    return status.equalsIgnoreCase("open")
        || status.equalsIgnoreCase("ready to start");
  }

  public double getProgress() {
    if (subItems.size() <= 0) {
      return isClosed() ? 1.0 : isDevComplete() ? 0.9: (isOpen() ? 0.0:0.5);
    } else {
      double progress = 0;
      for (Item subItem : subItems) {
        progress += subItem.getProgress();
      }
      return progress / subItems.size();
    }
  }

  private boolean isDevComplete() {
    return status.equalsIgnoreCase("ready for devqa")
        || status.equalsIgnoreCase("ready for dev qa");
  }
}
