package com.lmitsoftware.ctf.model.poc;

import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.lmitsoftware.ctf.CTFPluginNodesModule;

public class PocNodesProvider implements Provider<AbstractNode> {
  private static final Logger log = Logger.getLogger(PocNodesProvider.class);
  private Injector injector;
  private PluginConfig config;
  private UrlPath pathHelper;

  public interface Factory {
    public PocNodesProvider create(UrlPath pathHelper, PluginConfig config);
  }

  @Inject
  public PocNodesProvider(Injector injector, @Assisted UrlPath pathHelper,
      @Assisted PluginConfig config) {
    this.pathHelper = pathHelper;
    this.injector = injector;
    this.config = config;
  }

  @Override
  public AbstractNode get() {
    Injector childInjector = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(UrlPath.class).toInstance(pathHelper);
        install(new CTFPluginNodesModule(config));
      }
    });

    return childInjector.getInstance(ProjectsSprintPlan.class);
  }
}
