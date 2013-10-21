package com.lmitsoftware.ctf.model.poc;

import java.rmi.RemoteException;
import java.util.HashMap;

import mobi.jenkinsci.cache.LazyCacheMap;

import com.google.inject.Inject;
import com.lmitsoftware.ctf.model.poc.ProjectSprintDetails.Factory;

public class ProjectSprintDetailsCache {
  private static final long TTL = 5 * 60 * 1000L; // 5 mins TTL
  private static HashMap<String, ProjectSprintDetails> sprintDetailCache = null;
  private Factory detailsFactory;

  public class Loader implements
      LazyCacheMap.Loader<String, ProjectSprintDetails> {
    @SuppressWarnings("unchecked")
    @Override
    public <T> T load(String key,
        Class<T> valueType) throws RemoteException {
      if(valueType.equals(ProjectSprintDetails.class)) {
        String[] keyParts = key.split("\\|");
        String title = keyParts[0];
        String projectPath = keyParts[1];
        String folderPath = keyParts[2];
        String boardId = (keyParts.length > 3 ? keyParts[3]:"");
        String releaseId = (keyParts.length > 4 ? keyParts[4]:"");
        return (T) detailsFactory.create(title, projectPath, folderPath, boardId, releaseId);
      } else {
        return null;
      }
    }
  }

  @Inject
  public ProjectSprintDetailsCache(ProjectSprintDetails.Factory detailsFactory) {
    this.detailsFactory = detailsFactory;
  }

  public ProjectSprintDetails create(String title, String projectPath,
      String folderPath, String boardId, String releaseId) {
    String key = title + "|" + projectPath + "|" + folderPath + "|" + boardId + "|" + releaseId;
    return cacheSingleton().get(key);
  }

  private synchronized HashMap<String, ProjectSprintDetails> cacheSingleton() {
    if (sprintDetailCache == null) {
      sprintDetailCache =
          new LazyCacheMap<String, ProjectSprintDetails>(TTL, new Loader(), ProjectSprintDetails.class);
    }
    return sprintDetailCache;
  }
}
