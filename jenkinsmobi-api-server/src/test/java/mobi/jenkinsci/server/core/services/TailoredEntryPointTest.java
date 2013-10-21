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
package mobi.jenkinsci.server.core.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.PluginConfig;

import mobi.jenkinsci.server.Config;
import org.ini4j.Ini;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TailoredEntryPointTest {

  private static final String A_PLUGIN_ENTRY_POINT_TITLE = "aPluginEntryPoint";
  private static final String A_USER = "aUsername";
  private static final String A_PLUGIN_NAME = "aPluginName";
  private static final String A_PLUGIN_TYPE = "aPluginType";

  private final ItemNode pluginEntryPoint = new ItemNode(
      A_PLUGIN_ENTRY_POINT_TITLE);

  @Mock
  private Config mockConfig;

  @Mock
  private Ini mockIni;

  @Mock
  private PluginLoader mockPluginLoader;

  @Mock
  private Account mockAccount;

  private TailoredEntryPoint tailoredEntryPoint;

  @Before
  public void setUp() throws Exception {
    when(mockConfig.loadIni(Config.ACCESS_CONTROL_CONFIG)).thenReturn(mockIni);
    when(mockAccount.getName()).thenReturn(A_USER);
    tailoredEntryPoint =
        new TailoredEntryPoint(mockConfig, mockAccount, new PluginConfig(
            A_PLUGIN_NAME, A_PLUGIN_TYPE).getKey(), pluginEntryPoint);
  }

  @Test
  public void pluginHasTailoredUserRootPath() {
    when(mockIni.get(A_USER, A_PLUGIN_NAME + "." + pluginEntryPoint))
        .thenReturn("/root/path");

    assertThat(tailoredEntryPoint, hasProperty("path", endsWith("/root/path")));
  }

  @Test
  public void entryPointIsInvisibleWhenThereIsNoTailoredEntry() {
    assertThat(tailoredEntryPoint, hasProperty("visible", is(false)));
  }

  @Test
  public void entryPointIsVisibleWhenThereIsATailoredEntry() {
    when(mockIni.get(A_USER, A_PLUGIN_NAME + "." + pluginEntryPoint))
        .thenReturn("/anypath");

    assertThat(tailoredEntryPoint, hasProperty("visible", is(true)));
  }

  @Test
  public void returnOriginalIconTitleWhenThereIsTailoredPathButNotATailoredIconTitle() {
    when(mockIni.get(A_USER, A_PLUGIN_NAME + "." + pluginEntryPoint))
        .thenReturn("/anyPath");

    assertThat(tailoredEntryPoint,
        hasProperty("title", equalTo(A_PLUGIN_ENTRY_POINT_TITLE)));
  }

  @Test
  public void returnOriginalIconImageWhenThereIsNoTailoredIconImage() {
    pluginEntryPoint.setIcon("icon.png");

    assertThat(tailoredEntryPoint, hasProperty("icon", endsWith("icon.png")));
  }

  @Test
  public void returnTailoredIconTitleWhenThereIsTailoredIconTitle() {
    when(mockIni.get(A_USER, A_PLUGIN_NAME + "." + pluginEntryPoint))
        .thenReturn("/anyPath,My Title");

    assertThat(tailoredEntryPoint, hasProperty("title", equalTo("My Title")));
  }

  @Test
  public void pluginHasTailoredIconImage() {
    when(mockIni.get(A_USER, A_PLUGIN_NAME + "." + pluginEntryPoint))
        .thenReturn("/anyPath,Any title,myimage.png");

    assertThat(tailoredEntryPoint, hasProperty("icon", endsWith("myimage.png")));
  }

  @Test
  public void pluginHasDefaultTailoredRootPath() {
    when(
        mockIni.get(TailoredEntryPoint.DEFAULT_USER, A_PLUGIN_NAME + "."
            + pluginEntryPoint)).thenReturn("/default/path");


    assertThat(tailoredEntryPoint,
        hasProperty("path", endsWith("/default/path")));
  }
}
