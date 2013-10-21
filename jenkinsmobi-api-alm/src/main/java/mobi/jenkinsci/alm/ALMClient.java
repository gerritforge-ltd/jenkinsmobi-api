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

import java.rmi.RemoteException;



public interface ALMClient {

  void login(String username, String password) throws RemoteException;

  void logout() throws RemoteException;

  Project[] getProjects() throws RemoteException;

  SprintSummary getSprintSummary(String folderId)
      throws RemoteException;

  Sprint[] getSubSprintPlan(String sprintId)
      throws RemoteException;

  Sprint[] getFolderList(String projectId)
      throws RemoteException;

  String getProjectId(String projectPath) throws RemoteException;

  String getFolderId(String projectId, String folderPath)
      throws RemoteException;

  Item[] getFolderArtifacts(String folderId)
      throws RemoteException;
}
