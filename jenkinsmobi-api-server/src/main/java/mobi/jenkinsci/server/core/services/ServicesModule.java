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

import com.google.inject.assistedinject.FactoryModuleBuilder;
import mobi.jenkinsci.guice.DynamicList;

import com.google.inject.AbstractModule;
import mobi.jenkinsci.plugin.PluginFactory;
import mobi.jenkinsci.plugin.URLDownloader;
import mobi.jenkinsci.server.core.net.HttpClientURLDownloader;
import mobi.jenkinsci.server.core.net.NetModule;
import mobi.jenkinsci.server.upgrade.UpgradeConfig;

public class ServicesModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new NetModule());

    DynamicList.listOf(binder(), RequestCommand.class);


    bindFactory(TailoredEntryPoint.class, TailoredEntryPoint.Factory.class);
    bindFactory(UpgradeConfig.class, UpgradeConfig.Factory.class);

    bindProcessor(ImageRequestCommand.class);
    bindProcessor(DownloadRequestCommand.class);
    bindProcessor(DownloadRequestCommand.class);
    bindProcessor(ProxyRequestCommand.class);
    bindProcessor(PluginEntryPointRequestCommand.class);
    bindProcessor(UpgradeRequestCommand.class);
    bindProcessor(PluginRequestCommand.class);

    bind(PluginFactory.class).to(PluginLoader.class);
  }

  private void bindFactory(Class beanClass, Class factoryClass) {
    install(new FactoryModuleBuilder()
            .implement(beanClass, beanClass)
            .build(factoryClass));
  }

  private void bindProcessor(final Class<? extends RequestCommand> clazz) {
    DynamicList.bind(binder(), RequestCommand.class).to(clazz);
  }
}
