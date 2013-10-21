package com.lmitsoftware.ctf;

import mobi.jenkinsci.plugin.PluginConfig;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lmitsoftware.ctf.model.ProjectNode;
import com.lmitsoftware.ctf.model.ProjectSprintFolder;
import com.lmitsoftware.ctf.model.ProjectSprints;
import com.lmitsoftware.ctf.model.poc.ProjectSprint;
import com.lmitsoftware.ctf.model.poc.ProjectSprintDetails;

public class CTFPluginNodesModule extends AbstractModule {

  private PluginConfig config;

  public CTFPluginNodesModule(PluginConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(PluginConfig.class).toInstance(config);
    bind(CTFClient.class);
    install(new FactoryModuleBuilder().build(ProjectNode.Factory.class));
    install(new FactoryModuleBuilder().build(ProjectSprints.Factory.class));
    install(new FactoryModuleBuilder().build(ProjectSprintFolder.Factory.class));
    install(new FactoryModuleBuilder().build(ProjectSprint.Factory.class));
    install(new FactoryModuleBuilder().build(ProjectSprintDetails.Factory.class));
  }

}
