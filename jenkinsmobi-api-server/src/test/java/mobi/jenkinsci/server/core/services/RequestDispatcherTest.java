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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.guice.DynamicList;
import mobi.jenkinsci.model.AbstractNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(MockitoJUnitRunner.class)
public class RequestDispatcherTest {

  private AbstractModule guiceModule;

  @Mock
  private RequestCommand mockRequestConsumer1;

  @Mock
  private RequestCommand mockRequestConsumer2;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private AbstractNode mockResponse;

  @Mock
  private Account mockAccount;

  @Before
  public void setuUp() {
    guiceModule = new AbstractModule() {
      @Override
      protected void configure() {
        DynamicList.listOf(binder(), RequestCommand.class);
      }
    };
  }

  @Test
  public void shouldDispatchInvokeTheOnlyRequestConsumer() throws IOException {
    // given
    final Injector injector = createInjector(mockRequestConsumer1);
    when(mockRequestConsumer1.canProcess(mockRequest)).thenReturn(true);

    // when
    final RequestCommandDispatcher requestDispatcher =
        injector.getInstance(RequestCommandDispatcher.class);
    requestDispatcher.getResponse(mockAccount, mockRequest);

    // then
    verify(mockRequestConsumer1).canProcess(mockRequest);
    verify(mockRequestConsumer1).process(mockAccount, mockRequest);
  }

  @Test
  public void shouldReturnResponseFromRequestConsumer() throws IOException {
    // given
    final Injector injector = createInjector(mockRequestConsumer1);
    when(mockRequestConsumer1.canProcess(mockRequest)).thenReturn(true);
    when(mockRequestConsumer1.process(mockAccount, mockRequest)).thenReturn(
        mockResponse);

    // when
    final RequestCommandDispatcher requestDispatcher =
        injector.getInstance(RequestCommandDispatcher.class);
    final AbstractNode response =
        requestDispatcher.getResponse(mockAccount, mockRequest);

    // then
    assertThat(response, is(sameInstance(mockResponse)));
  }



  @Test
  public void shouldDispatchInvokeTheSecondRequestConsumer() throws IOException {
    // given
    final Injector injector =
        createInjector(mockRequestConsumer1, mockRequestConsumer2);
    when(mockRequestConsumer1.canProcess(mockRequest)).thenReturn(false);
    when(mockRequestConsumer2.canProcess(mockRequest)).thenReturn(true);

    // when
    final RequestCommandDispatcher requestDispatcher =
        injector.getInstance(RequestCommandDispatcher.class);
    requestDispatcher.getResponse(mockAccount, mockRequest);

    // then
    verify(mockRequestConsumer1).canProcess(mockRequest);
    verify(mockRequestConsumer1, never()).process(mockAccount, mockRequest);
    verify(mockRequestConsumer2).canProcess(mockRequest);
    verify(mockRequestConsumer2).process(mockAccount, mockRequest);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenNoRequestConsumersCanProcessTheRequest()
      throws IOException {
    // given
    final Injector injector =
        createInjector(mockRequestConsumer1, mockRequestConsumer2);

    // when
    injector.getInstance(RequestCommandDispatcher.class).getResponse(mockAccount,
        mockRequest);
  }

  private Injector createInjector(final RequestCommand... consumers) {
    return Guice.createInjector(guiceModule, new AbstractModule() {
      @Override
      protected void configure() {
        for (final RequestCommand requestConsumer : consumers) {
          DynamicList.bind(binder(), RequestCommand.class).toInstance(
              requestConsumer);
        }
      }
    });
  }

}
