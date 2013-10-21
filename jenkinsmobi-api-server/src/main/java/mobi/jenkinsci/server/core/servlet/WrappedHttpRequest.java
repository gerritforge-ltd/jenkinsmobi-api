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

import mobi.jenkinsci.net.UrlPath;

public class WrappedHttpRequest extends HttpServletRequestWrapper
implements HttpServletRequest {

  private final String basepath;
  private String replaceWith;
  private String newPathInfo;

  public WrappedHttpRequest(final HttpServletRequest instance, final String basepath) {
    super(instance);
    this.basepath = basepath;
  }

  public WrappedHttpRequest(final HttpServletRequest request, final String basepath,
      final String replaceWith) {
    this(request, basepath);
    this.replaceWith = replaceWith;
  }

  public WrappedHttpRequest(final HttpServletRequest request, final String basepath,
      final String replaceWith, final String newPathInfo) {
    this(request, basepath, replaceWith);
    this.newPathInfo = newPathInfo;
  }

  public WrappedHttpRequest(final HttpServletRequest request) {
    this(request, "/" + new UrlPath(request).getPluginId()
        + "$", "/");
  }

  @Override
  public String getRequestURI() {
    String uri = super.getRequestURI();
    if (basepath != null && replaceWith != null) {
      final int basepathPos = uri.indexOf(basepath);
      if(basepathPos >= 0) {
        uri = uri.substring(basepathPos + basepath.length());
        uri = replaceWith + uri;
      }
    }
    if(newPathInfo != null) {
      final String origPathInfo = super.getPathInfo();
      uri = uri.replaceAll(origPathInfo, newPathInfo);
    }

    return uri;
  }

  @Override
  public String getPathInfo() {
    if(newPathInfo!=null){
      return newPathInfo;
    }

    String fullPathInfo = super.getPathInfo();
    if(fullPathInfo == null) {
      fullPathInfo = "/";
    }

    while(fullPathInfo.indexOf("//") >= 0) {
      fullPathInfo = fullPathInfo.replaceAll("//","/");
    }

    if (basepath != null) {
      if (replaceWith == null) {
        fullPathInfo = fullPathInfo.substring(basepath.length());
      } else {
        fullPathInfo = fullPathInfo.replace(basepath, replaceWith);
      }
    }
    return fullPathInfo;
  }
}
