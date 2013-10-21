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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.model.Layout;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginConfig.Key;

import mobi.jenkinsci.server.upgrade.UpgradeNode;
import mobi.jenkinsci.server.upgrade.UpgradeNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PluginEntryPointRequestCommandTest {

  private static final String A_PLUGIN_NAME = "aPluginName";
  private static final String A_PLUGIN_TYPE = "aPluginType";
  private static final boolean VISIBLE = true;
  private static final boolean HIDDEN = false;

  @Mock
  private UpgradeNodeFactory mockUpgradeNodeFactory;

  @Mock
  private UpgradeNode mockUpgradeNode;

  @Mock
  private PluginLoader mockPluginLoader;

  @Mock
  private Plugin mockPlugin;

  @Mock
  private TailoredEntryPoint.Factory mockTailorPluginFactory;

  @Mock
  private TailoredEntryPoint mockTailoredEntryPoint;

  @Mock
  private Account mockAccount;

  @Mock
  private HttpServletRequest mockServletRequest;

  @InjectMocks
  private PluginEntryPointRequestCommand pluginEntryPointRequestCommand;

  @Before
  public void setUp() {
    when(mockAccount.listPluginsConfigs()).thenReturn(
        new ArrayList<PluginConfig>());
  }

  @Test
  public void shouldProcessRootPathRequest() {
    mockServletRequestWithRootPathInfo();

    assertThat(pluginEntryPointRequestCommand.canProcess(mockServletRequest),
        equalTo(true));
  }

  private void mockServletRequestWithRootPathInfo() {
    when(mockServletRequest.getPathInfo()).thenReturn("/");
  }

  @Test
  public void shouldNotProcessDeepPathRequest() {
    when(mockServletRequest.getPathInfo()).thenReturn("/myplugin$entry/");

    assertThat(pluginEntryPointRequestCommand.canProcess(mockServletRequest),
        equalTo(false));
  }

  @Test
  public void shouldReturnNotNullRootNodeWhenGettingEntryPoints()
      throws IOException {
    mockServletRequestWithRootPathInfo();

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        notNullValue(AbstractNode.class));
  }

  @Test
  public void shouldReturnItemNodeIconsRootNodeWhenGettingEntryPoints()
      throws IOException {
    mockServletRequestWithRootPathInfo();

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        allOf(instanceOf(ItemNode.class),
            hasProperty("layout", equalTo(Layout.ICONS))));
  }

  @Test
  public void shouldReturnUpgradeSubNodeWhenUpgradeIsRequired()
      throws IOException {
    mockServletRequestWithRootPathInfo();
    when(mockUpgradeNodeFactory.getAvailableUpgrade(mockServletRequest, mockAccount))
        .thenReturn(mockUpgradeNode);

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        hasProperty("payload", hasItems(mockUpgradeNode)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReturnVisibleEntryPointSubNodeFromPlugin()
      throws IOException {
    mockServletRequestWithRootPathInfo();
    mockAccountWithOnePluginConfig();
    final ItemNode entryPoint = new ItemNode("anEntryPoint");
    mockPluginWithOneEntryPoint(entryPoint);
    mockTailoredEntryPoint(entryPoint, VISIBLE);

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        hasProperty("payload", hasItems(any(ItemNode.class))));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReturnOnlyMandatoryUpgradeNodeEvenWhenHasVisibleEntryPointSubNodeFromPlugin()
      throws IOException {
    mockServletRequestWithRootPathInfo();
    mockAccountWithOnePluginConfig();
    final ItemNode entryPoint = new ItemNode("anEntryPoint");
    mockPluginWithOneEntryPoint(entryPoint);
    mockTailoredEntryPoint(entryPoint, VISIBLE);

    when(mockUpgradeNodeFactory.getAvailableUpgrade(mockServletRequest, mockAccount))
        .thenReturn(mockUpgradeNode);
    when(mockUpgradeNode.isMandatory()).thenReturn(true);

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        hasProperty("payload", hasItems(sameInstance(mockUpgradeNode))));
  }

  @Test
  public void shouldNotReturnHiddenEntryPointSubNodeFromPlugin()
      throws IOException {
    mockServletRequestWithRootPathInfo();
    mockAccountWithOnePluginConfig();
    final ItemNode entryPoint = new ItemNode("anEntryPoint");
    mockPluginWithOneEntryPoint(entryPoint);
    mockTailoredEntryPoint(entryPoint, HIDDEN);

    assertThat(
        pluginEntryPointRequestCommand.process(mockAccount, mockServletRequest),
        hasProperty("payload", nullValue()));
  }

  private void mockTailoredEntryPoint(final ItemNode entryPoint, final boolean visible) {
    when(
        mockTailorPluginFactory.get(Mockito.same(mockAccount),
            Mockito.any(Key.class),
            Mockito.same(entryPoint))).thenReturn(mockTailoredEntryPoint);
    when(mockTailoredEntryPoint.isVisible()).thenReturn(visible);
  }

  private void mockPluginWithOneEntryPoint(final ItemNode entryPoint)
      throws IOException {
    when(mockPlugin.getEntryPoints(Mockito.any(PluginConfig.class)))
        .thenReturn(Arrays.asList(entryPoint));
  }

  private void mockAccountWithOnePluginConfig() {
    when(mockAccount.listPluginsConfigs()).thenReturn(
        Arrays.asList(new PluginConfig(A_PLUGIN_NAME, A_PLUGIN_TYPE)));
    when(mockPluginLoader.get(A_PLUGIN_TYPE)).thenReturn(mockPlugin);
  }
}
