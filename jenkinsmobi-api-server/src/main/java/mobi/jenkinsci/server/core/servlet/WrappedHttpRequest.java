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
package mobi.jenkinsci.server.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import mobi.jenkinsci.net.UrlPath;

public class WrappedHttpRequest extends HttpServletRequestWrapper implements HttpServletRequest {

  private final String basepath;
  private final String replaceWith;

  public WrappedHttpRequest(final HttpServletRequest request, final String basepath, final String replaceWith) {
    super(request);

    this.basepath = basepath;
    this.replaceWith = replaceWith;
  }

  @Override
  public String getRequestURI() {
    String uri = super.getRequestURI();

    final int basepathPos = uri.indexOf(basepath);
    if (basepathPos >= 0) {
      uri = uri.substring(basepathPos + basepath.length());
      uri = replaceWith + uri;
    }

    return uri;
  }

  @Override
  public String getPathInfo() {
    String fullPathInfo = Objects.firstNonNull(super.getPathInfo(), "/");

    if (fullPathInfo.startsWith(basepath)) {
      fullPathInfo = fullPathInfo.replace(basepath, replaceWith);
    }

    return fullPathInfo;
  }
}
