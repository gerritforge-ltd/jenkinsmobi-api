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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.guice.DynamicList;
import mobi.jenkinsci.model.AbstractNode;

import com.google.inject.Inject;

public class RequestCommandDispatcher {

  private final DynamicList<RequestCommand> requestProcessors;

  @Inject
  public RequestCommandDispatcher(
      final DynamicList<RequestCommand> requestProcessors) {
    this.requestProcessors = requestProcessors;
  }

  public AbstractNode getResponse(final Account account,
      final HttpServletRequest request) throws IOException {

    for (final RequestCommand requestProcessor : requestProcessors) {
      if (requestProcessor.canProcess(request)) {
        return requestProcessor.process(account, request);
      }
    }

    throw new ResourceNotFoundException(
        "Cannot find any processor to manage request "
            + request.getRequestURI());
  }
}
