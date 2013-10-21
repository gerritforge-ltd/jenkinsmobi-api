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
import mobi.jenkinsci.net.UrlPath;

import com.google.gson.annotations.SerializedName;

public class Computer extends JenkinsItem {

  @SerializedName("offline")
  private boolean offline;
  @SerializedName("idle")
  private boolean idle;
  @SerializedName("numExecutors")
  private int numExecutors;

  @SerializedName("monitorData")
  private MonitorData monitorData;

  public boolean isOffline() {
    return offline;
  }

  public void setOffline(boolean offline) {
    this.offline = offline;
  }

  public boolean isIdle() {
    return idle;
  }

  public void setIdle(boolean idle) {
    this.idle = idle;
  }

  public int getNumExecutors() {
    return numExecutors;
  }

  public void setNumExecutors(int numExecutors) {
    this.numExecutors = numExecutors;
  }

  public MonitorData getMonitorData() {
    return monitorData;
  }

  public void setMonitorData(MonitorData monitorData) {
    this.monitorData = monitorData;
  }

  public class MonitorData {

    @SerializedName("hudson.node_monitors.SwapSpaceMonitor")
    private SwapSpaceMonitor swapSpaceMonitor;
    @SerializedName("hudson.node_monitors.ArchitectureMonitor")
    private String architectureMonitor;

    public SwapSpaceMonitor getSwapSpaceMonitor() {
      return swapSpaceMonitor;
    }

    public void setSwapSpaceMonitor(SwapSpaceMonitor swapSpaceMonitor) {
      this.swapSpaceMonitor = swapSpaceMonitor;
    }

    public String getArchitectureMonitor() {
      return architectureMonitor;
    }

    public void setArchitectureMonitor(String architectureMonitor) {
      this.architectureMonitor = architectureMonitor;
    }
  }

  public class SwapSpaceMonitor {

    @SerializedName("availablePhysicalMemory")
    private long availablePhysicalMemory;
    @SerializedName("availableSwapSpace")
    private long availableSwapSpace;
    @SerializedName("totalPhysicalMemory")
    private long totalPhysicalMemory;
    @SerializedName("totalSwapSpace")
    private long totalSwapSpace;

    public long getAvailablePhysicalMemory() {
      return availablePhysicalMemory;
    }

    public void setAvailablePhysicalMemory(long availablePhysicalMemory) {
      this.availablePhysicalMemory = availablePhysicalMemory;
    }

    public long getAvailableSwapSpace() {
      return availableSwapSpace;
    }

    public void setAvailableSwapSpace(long availableSwapSpace) {
      this.availableSwapSpace = availableSwapSpace;
    }

    public long getTotalPhysicalMemory() {
      return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(long totalPhysicalMemory) {
      this.totalPhysicalMemory = totalPhysicalMemory;
    }

    public long getTotalSwapSpace() {
      return totalSwapSpace;
    }

    public void setTotalSwapSpace(long totalSwapSpace) {
      this.totalSwapSpace = totalSwapSpace;
    }
  }

  @Override
  public ItemNode toAbstractNode(String urlPrefix) {

    ItemNode result = new ItemNode();
    result.setLayout(Layout.LIST);
    result.setVersion(ItemNode.API_VERSION);
    result.setPath(UrlPath.normalizePath(path));
    result.setDescriptionAlign(Alignment.BOTTOM);
    result.setTitle(displayName);
    result.setIcon(result.getPath()+"?image="+(isOffline() ? "icons/computer_offline.png"
        : "icons/computer_online.png"));
    if (getMonitorData().getArchitectureMonitor() != null) {
      result.setDescription(getNumExecutors() + " executors - "
          + getMonitorData().getArchitectureMonitor());
    } else {
      result.setDescription(getNumExecutors() + " executors - N/A");
    }

    if (getMonitorData().getArchitectureMonitor() != null) {

      ItemNode childNode =
          new ItemNode("Operating System", getMonitorData()
              .getArchitectureMonitor());
      childNode.setDescriptionAlign(Alignment.RIGHT);
      result.addNode(childNode);
    }

    ItemNode childNode =
        new ItemNode("Status", isOffline() ? "OFF-LINE" : "ON-LINE");
    childNode.setDescriptionAlign(Alignment.RIGHT);
    childNode.setDescriptionColor(isOffline() ? "#FF0000" : "#00FF00");
    result.addNode(childNode);

    childNode =
        new ItemNode("Number of executors",
            Integer.toString(getNumExecutors()));
    childNode.setDescriptionAlign(Alignment.RIGHT);
    result.addNode(childNode);

    if (getMonitorData().getSwapSpaceMonitor() != null) {
      long totRam =
          getMonitorData().getSwapSpaceMonitor().getTotalPhysicalMemory() / 1024 / 1024;
      long availRam =
          getMonitorData().getSwapSpaceMonitor().getAvailablePhysicalMemory() / 1024 / 1024;

      childNode =
          new ItemNode("RAM", "" + availRam + "/" + totRam + " MB");
      childNode.setDescriptionAlign(Alignment.RIGHT);
      result.addNode(childNode);


      long totSwap =
          getMonitorData().getSwapSpaceMonitor().getTotalSwapSpace() / 1024 / 1024;
      long availSwap =
          getMonitorData().getSwapSpaceMonitor().getAvailableSwapSpace() / 1024 / 1024;

      childNode =
          new ItemNode("SWAP", "" + availSwap + "/" + totSwap
              + " MB");
      childNode.setDescriptionAlign(Alignment.RIGHT);
      result.addNode(childNode);
    }
    
    result.setLeaf(true);

    return result;
  }
}
