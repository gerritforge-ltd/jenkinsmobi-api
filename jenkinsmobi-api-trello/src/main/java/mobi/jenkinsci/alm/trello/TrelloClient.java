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
package mobi.jenkinsci.alm.trello;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.alm.Project;
import mobi.jenkinsci.alm.Sprint;
import mobi.jenkinsci.alm.SprintSummary;
import mobi.jenkinsci.alm.trello.nodes.Card;
import mobi.jenkinsci.alm.trello.nodes.Card.CheckItemState;
import mobi.jenkinsci.alm.trello.nodes.CardList;
import mobi.jenkinsci.alm.trello.nodes.CheckItem;
import mobi.jenkinsci.alm.trello.nodes.Member;
import mobi.jenkinsci.alm.trello.nodes.Organisation;
import mobi.jenkinsci.alm.trello.nodes.TrelloObject;
import mobi.jenkinsci.net.*;
import mobi.jenkinsci.net.LazyCacheMap.Loader;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TrelloClient implements ALMClient {
  private static final Logger log = Logger.getLogger(TrelloClient.class);
  
  private HttpClient http;
  public static final int VERSION = 1;
  private String url;
  private String appKey;
  private String token;
  private Member loggedInMember;
  private HttpLoader<TrelloObject> httpLoader = new HttpLoader<TrelloObject>(TrelloObject.class);
  private HttpLoader<TrelloObject[]> httpArrayLoader = new HttpLoader<TrelloObject[]>(TrelloObject[].class);
  private LazyCacheMap<String, TrelloObject> jsonCache;
  private LazyCacheMap<String, TrelloObject[]> jsonArrayCache;
  
  public class HttpLoader<V> implements Loader<String, V> {
    
    private Class<V> valueClass;
    
    public HttpLoader(Class<V> valueClass) {
      this.valueClass = valueClass;
    }

    private <T> T execute(String api, Class<T> returnType) throws RemoteException {
      return executeHttpAPi(getHttpApi(api), returnType);
    }

    private <T> T executeHttpAPi(HttpUriRequest httpApi, Class<T> returnClass)
        throws RemoteException {
      try {
        HttpResponse response = http.execute(httpApi);
        int status = response.getStatusLine().getStatusCode();
        log.debug("TRELLO-API: " + httpApi.getURI() + " RETURNED HTTP-STATUS:"
            + status);
        if (status == HttpURLConnection.HTTP_OK) {
          return gson.fromJson(new InputStreamReader(response.getEntity()
              .getContent()), returnClass);
        }
        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
          return null;
        } else {
          throw new RemoteException(httpApi.getURI()
              + " FAILED with HTTP Status = " + status);
        }
      } catch (Exception e) {
        throw new RemoteException(httpApi.getURI() + " FAILED", e);
      }
    }

    @Override
    public V load(String key) throws RemoteException {
      return execute(key, valueClass);
    }
  }
  
  private static Gson gson;
  static {
    GsonBuilder gsonB = new GsonBuilder();
    gsonB
        .registerTypeAdapter(GregorianCalendar.class, new CalendarSerializer());
    gson = gsonB.create();
  }

  public static class CalendarSerializer implements JsonSerializer<Calendar>,
      JsonDeserializer<Calendar> {
    private static final SimpleDateFormat calendarFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public Calendar deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Calendar cal = Calendar.getInstance();
      String calString = json.getAsString();
      try {
        cal.setTime(calendarFormat.parse(calString));
      } catch (ParseException e) {
        throw new JsonParseException("Unparseable date " + calString, e);
      }
      return cal;

    }

    @Override
    public JsonElement serialize(Calendar src, Type typeOfSrc,
        JsonSerializationContext context) {
      return new JsonPrimitive(calendarFormat.format(src.getTime()));
    }
  }

  @Inject
  public TrelloClient(PluginConfig pluginConf, long cacheTTL, HttpClientFactory httpClientFactory) throws
          RemoteException {
    Map<String, String> options =
        Objects.firstNonNull(pluginConf.getOptions(),
            new HashMap<String, String>());
    this.url =
        Objects.firstNonNull(options.get("trellourl"), pluginConf.getUrl());
    if (!url.endsWith("/")) {
      url += "/";
    }

    String appKey =
        Objects.firstNonNull(options.get("key"), pluginConf.getUsername());
    String token =
        Objects.firstNonNull(options.get("token"), pluginConf.getPassword());
    login(appKey, token);
    
    jsonCache = new LazyCacheMap<String, TrelloObject>(httpLoader, cacheTTL);
    jsonArrayCache = new LazyCacheMap<String, TrelloObject[]>(httpArrayLoader, cacheTTL);
    http = httpClientFactory.getHttpClient();
  }

  @Override
  public void login(String appKey, String token) throws RemoteException {
    this.appKey = appKey;
    this.token = token;
    loggedInMember = (Member) jsonCache.get("members/me");
  }



  @Override
  public void logout() throws RemoteException {
    this.appKey = null;
    this.token = null;
    this.loggedInMember = null;
  }

  @Override
  public Project[] getProjects() throws RemoteException {
    String[] orgsIds = loggedInMember.idOrganizations;
    Project[] projects = new Project[orgsIds.length];
    for (int i = 0; i < projects.length; i++) {
      Organisation organisation = 
          (Organisation) jsonCache.get("organizations/" + orgsIds[i]);
      projects[i] =
          new Project(orgsIds[i], organisation.displayName, orgsIds[i]);
    }

    return projects;
  }

  @Override
  public SprintSummary getSprintSummary(String folderId) throws RemoteException {
    return null;
  }

  @Override
  public Sprint[] getSubSprintPlan(String sprintId) throws RemoteException {
    return new Sprint[] {};
  }

  @Override
  public Sprint[] getFolderList(String projectId) throws RemoteException {
    TrelloObject[] boards =
        jsonArrayCache.get("organizations/" + projectId + "/boards");
    Sprint[] sprints = new Sprint[boards.length];
    for (int i = 0; i < boards.length; i++) {
      sprints[i] = new Sprint(boards[i].id, boards[i].name, boards[i].id);
    }
    return sprints;
  }

  @Override
  public String getProjectId(String projectPath) throws RemoteException {
    return projectPath;
  }

  @Override
  public String getFolderId(String projectId, String folderPath)
      throws RemoteException {
    return folderPath;
  }

  @Override
  public Item[] getFolderArtifacts(String folderId) throws RemoteException {
    if (folderId == null || folderId.trim().length() <= 0) {
      return new Item[] {};
    }
    Card[] cards = (Card[]) jsonArrayCache.get("boards/" + folderId + "/cards");
    if (cards == null) {
      return new Item[] {};
    }

    Item[] items = new Item[cards.length];
    for (int i = 0; i < items.length; i++) {
      Card card = cards[i];
      CardList list = (CardList) jsonCache.get("lists/" + card.idList);
      String cardMembers = getCardMembers(card);
      Item item =
          new Item(card.id, card.closed ? "Closed" : list.name, card.name, 3,
              card.desc, "", cardMembers, null, null, "cards");
      if (card.idChecklists.length > 0) {
        item.addSubItems(addChecklists(card.idChecklists, card, cardMembers));
      }
      items[i] = item;
    }
    return items;
  }

  private String getCardMembers(Card card) throws RemoteException {
    StringBuilder members = new StringBuilder();
    for (String memberId : card.idMembers) {
      Member member = (Member) jsonCache.get("members/" + memberId);
      if (members.length() > 0) {
        members.append("; ");
      }
      members.append(member.fullName);
    }
    return members.toString();
  }

  private ArrayList<Item> addChecklists(String[] checkLists, Card card,
      String cardMembers) throws RemoteException {
    ArrayList<Item> subItems = new ArrayList<Item>();
    for (String checklistId : checkLists) {
      CheckItem[] checkList =
          (CheckItem[]) jsonArrayCache.get("checklists/" + checklistId + "/checkItems");
      if (checkList != null && checkList.length > 0) {
        for (CheckItem checkItem : checkList) {
          subItems.add(new Item(checkItem.id, checkItem.name, getStatus(
              checkItem, card.checkItemStates)));
        }
      }
    }
    return subItems;
  }

  private boolean getStatus(CheckItem checkItem,
      Card.CheckItemState[] checklistStatuses) {
    for (CheckItemState checkItemStatus : checklistStatuses) {
      if (checkItemStatus.idCheckItem.equals(checkItem.id)) {
        return checkItemStatus.state.equalsIgnoreCase("complete");
      }
    }

    return false;
  }

  private HttpUriRequest getHttpApi(String apiPath) {
    if (apiPath.startsWith("/")) {
      apiPath = apiPath.substring(1);
    }

    return new HttpGet(url + VERSION + "/" + apiPath + "?key=" + appKey
        + "&token=" + token);
  }
}
