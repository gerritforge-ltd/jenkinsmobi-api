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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.server.upgrade.UpgradeNode;

import mobi.jenkinsci.server.upgrade.UpgradeNodeFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeRequestCommandTest {

  @Mock
  private UpgradeNodeFactory mockUpgradeNodeFactory;

  @Mock
  private UpgradeNode mockUpgradeNode;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private Account mockAccount;

  @Mock
  private AbstractNode mockUpgradeAbstractNode;

  @InjectMocks
  private UpgradeRequestCommand upgradeRequestCommand;

  @Test
  public void shouldProcessUpgradeRequest() {
    mockUpgradeRequest();

    assertThat(upgradeRequestCommand.canProcess(mockRequest), is(true));
  }

  private void mockUpgradeRequest() {
    when(mockRequest.getPathInfo()).thenReturn("/upgrade");
  }

  @Test
  public void shouldNotProcessNonUpgradeRequest() {
    when(mockRequest.getPathInfo()).thenReturn("/anotherRequest");

    assertThat(upgradeRequestCommand.canProcess(mockRequest), is(false));
  }

  @Test
  public void returnsUpgradeNodeFromUpgradePathRequest() throws IOException {
    mockUpgradeRequest();
    when(mockUpgradeNodeFactory.getAvailableUpgrade(mockRequest, mockAccount))
        .thenReturn(mockUpgradeNode);
    when(mockUpgradeNode.getNode(mockRequest)).thenReturn(
        mockUpgradeAbstractNode);

    assertThat(upgradeRequestCommand.process(mockAccount, mockRequest),
        sameInstance(mockUpgradeAbstractNode));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenNoUpgradeAvailable()
      throws IOException {
    mockUpgradeRequest();
    when(mockUpgradeNodeFactory.getAvailableUpgrade(mockRequest, mockAccount))
        .thenReturn(null);

    upgradeRequestCommand.process(mockAccount, mockRequest);
  }

}
