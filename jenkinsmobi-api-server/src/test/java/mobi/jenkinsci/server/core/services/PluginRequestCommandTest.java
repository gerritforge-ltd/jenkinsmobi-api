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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static mobi.jenkinsci.model.ItemNode.*;

import java.io.IOException;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.server.core.servlet.WrappedHttpRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PluginRequestCommandTest {

  private static final String A_PLUGIN_NAME = "aPluginName";
  private static final String A_PLUGIN_TYPE = "aPluginType";
  private static final String A_PLUGIN_ENTRY_POINT = "entryPoint";
  private static final String PLUGIN_REQUEST_ROOT_PATH = A_PLUGIN_NAME + "$"
      + A_PLUGIN_ENTRY_POINT;

  private final PluginConfig pluginConfig = new PluginConfig(A_PLUGIN_NAME,
      A_PLUGIN_TYPE);

  @Mock
  private PluginLoader mockPluginLoader;

  @Mock
  private Account mockAccount;

  @Mock
  private Plugin mockPlugin;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private AbstractNode mockResponse;

  @Mock
  private AbstractNode mockAbstractNode;

  @InjectMocks
  private PluginRequestCommand pluginRequestCommand;

  @Before
  public void setUp() {
    when(mockPluginLoader.getPlugin(A_PLUGIN_TYPE)).thenReturn(mockPlugin);
    when(mockAccount.getPluginConfig(A_PLUGIN_NAME)).thenReturn(pluginConfig);
  }

  @Test
  public void shouldAcceptToProcessPluginRequest() {
    mockPluginRootNodeRequest();

    assertThat(pluginRequestCommand.canProcess(mockRequest), is(true));
  }

  private void mockPluginRootNodeRequest() {
    mockPluginRequest("/");
  }

  private void mockPluginRequest(final String path) {
    when(mockRequest.getPathInfo()).thenReturn(
        "/" + PLUGIN_REQUEST_ROOT_PATH + path);
  }

  @Test
  public void shouldNotAcceptToProcessNonPluginRequests() {
    when(mockRequest.getPathInfo()).thenReturn("/this/is/another/request");

    assertThat(pluginRequestCommand.canProcess(mockRequest), is(false));
  }

  @Test
  public void shouldDispatchRequestToPlugin() throws IOException {
    mockPluginRootNodeRequest();
    when(
        mockPlugin.processRequest(any(Account.class),
            any(HttpServletRequest.class), any(PluginConfig.class)))
        .thenReturn(mockResponse);

    pluginRequestCommand.process(mockAccount, mockRequest);

    verify(mockPlugin).processRequest(same(mockAccount),
        any(WrappedHttpRequest.class), same(pluginConfig));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenPluginReturnsNull()
      throws IOException {
    mockPluginRootNodeRequest();
    when(
        mockPlugin.processRequest(any(Account.class),
            any(HttpServletRequest.class), any(PluginConfig.class)))
        .thenReturn(null);

    pluginRequestCommand.process(mockAccount, mockRequest);
  }

  @Test
  public void shouldCutResponseTreeToRequestPath() throws IOException {
    final ItemNode leafNode = itemNodeBuilder("Leaf").path("leaf").build();
    final ItemNode pluginResponse =
        itemNodeBuilder("root").path(PLUGIN_REQUEST_ROOT_PATH).payload(leafNode)
            .build();
    mockPluginRequest("/leaf");
    when(
        mockPlugin.processRequest(any(Account.class),
            any(HttpServletRequest.class), any(PluginConfig.class)))
        .thenReturn(pluginResponse);

    final AbstractNode response =
        pluginRequestCommand.process(mockAccount, mockRequest);

    assertThat(response, instanceOf(ItemNode.class));
    assertReflectionEquals(leafNode, response);
  }

  @Test
  public void shouldReturnOriginalResponseNode() throws IOException {
    mockPluginRequest("/leaf");
    when(
        mockPlugin.processRequest(any(Account.class),
            any(HttpServletRequest.class), any(PluginConfig.class)))
        .thenReturn(mockAbstractNode);

    final AbstractNode response =
        pluginRequestCommand.process(mockAccount, mockRequest);

    assertThat(response, sameInstance(mockAbstractNode));
  }
}
