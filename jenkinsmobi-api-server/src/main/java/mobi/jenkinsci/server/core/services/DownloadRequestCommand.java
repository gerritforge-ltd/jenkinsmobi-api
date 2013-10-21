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

import com.google.common.base.Objects;
import com.google.inject.Inject;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.Plugin;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.plugin.PluginLoader;
import mobi.jenkinsci.plugin.URLDownloader;

public class DownloadRequestCommand implements RequestCommand {

  private final PluginLoader pluginLoader;

  private final URLDownloader urlDownloader;

  @Inject
  public DownloadRequestCommand(final PluginLoader pluginLoader,
      final URLDownloader urlDownloader) {
    this.pluginLoader = pluginLoader;
    this.urlDownloader = urlDownloader;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return getDownloadTargetUrl(request) != null;
  }

  private String getDownloadTargetUrl(final HttpServletRequest request) {
    return request.getParameter("download");
  }

  @Override
  public AbstractNode process(final Account account,
      final HttpServletRequest request) throws IOException {
    final PluginConfig config =
        account.getPluginConfig(new UrlPath(request).getPluginId());
    final Plugin plugin = pluginLoader.getPlugin(config.getKey().getType());

    final String url = getDownloadTargetUrl(request);
    final URLDownloader downloader = getDownloader(plugin);
    final RawBinaryNode result =
        downloader.internalQueryForDownload(request, url, account, config);

    result.setHttpContentType(Objects.firstNonNull(result.getHttpContentType(),
        "application/octet-stream"));

    return result;
  }

  private URLDownloader getDownloader(final Plugin plugin) {
    return (plugin != null && URLDownloader.class.isAssignableFrom(plugin
        .getClass())) ? (URLDownloader) plugin : urlDownloader;
  }

}
