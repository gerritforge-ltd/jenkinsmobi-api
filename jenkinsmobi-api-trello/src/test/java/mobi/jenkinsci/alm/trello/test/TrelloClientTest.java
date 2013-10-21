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
package mobi.jenkinsci.alm.trello.test;

import junit.framework.TestCase;
import mobi.jenkinsci.alm.ALMClient;
import mobi.jenkinsci.alm.Item;
import mobi.jenkinsci.alm.Project;
import mobi.jenkinsci.alm.Sprint;
import mobi.jenkinsci.alm.trello.TrelloClient;

import mobi.jenkinsci.net.HttpClientFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Unit test for simple App.
 */
public class TrelloClientTest extends TestCase {
  private static final String APP_KEY = "9609d7baa441df0d0100376840eaf365";
  private static final String TOKEN =
      "6fc10ab2240415e3c870531a95a202e9c0a335f1d70a6bd0b22c6caf683980bf";
  ALMClient client;

  @Before
  public void setUp() throws Exception {
    client = new TrelloClient(new TrelloPluginConfig("https://trello.com"), 1L, new HttpClientFactory() {

      @Override
      public HttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
      }

      @Override
      public HttpClient getBasicAuthHttpClient(URL url, String user, String password) throws MalformedURLException {
        return getHttpClient();
      }
    });

  }

  public void testLoginDoesNotFailWithValidAppKeyAndToken() throws Exception {
    client.login(APP_KEY, TOKEN);
  }

  public void testAccountHasOneProject() throws Exception {
    client.login(APP_KEY, TOKEN);
    Project[] projects = client.getProjects();
    assertNotNull(projects);
    assertEquals(1, projects.length);
  }

  public void testAccountHasOneProjectAndThreeSprints() throws Exception {
    client.login(APP_KEY, TOKEN);
    Project[] projects = client.getProjects();
    Sprint[] sprints = client.getFolderList(projects[0].id);
    assertNotNull(sprints);
    assertEquals(4, sprints.length);
  }

  public void testAccountFirstSprintHasAtLeastOneCard() throws Exception {
    client.login(APP_KEY, TOKEN);
    Project[] projects = client.getProjects();
    Sprint[] sprints = client.getFolderList(projects[0].id);
    Item[] items = client.getFolderArtifacts(sprints[0].id);
    assertNotNull(items);
    assertTrue(items.length > 0);
  }
}
