package com.lmitsoftware.ctf.model;

import java.util.List;

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

public class NodesProvider implements Provider<AbstractNode>{
  private static final Logger log = Logger.getLogger(NodesProvider.class);
  private Injector injector;
  private PluginConfig config;
  private UrlPath pathHelper;
  
  public interface Factory {
    public NodesProvider create(UrlPath pathHelper, PluginConfig config);
  }
  
  @Inject
  public NodesProvider(Injector injector, @Assisted UrlPath pathHelper, @Assisted PluginConfig config) {
    this.pathHelper = pathHelper;
    this.injector = injector;
    this.config = config;
  }

  @SuppressWarnings("unchecked")
  private Class<? extends AbstractNode> getComponentClass(
      String topComponent) {
    String[] componentWords = topComponent.split("_");
    StringBuffer componentClassName =
        new StringBuffer(NodesProvider.class.getPackage().getName() + ".");
    for (String word : componentWords) {
      componentClassName.append(Character.toUpperCase(word.charAt(0)));
      componentClassName.append(word.substring(1));
    }
    componentClassName.append("Node");

    try {
      return (Class<? extends AbstractNode>) Class.forName(componentClassName
          .toString());
    } catch (Exception e) {
      log.error("Cannot get component class for " + topComponent, e);
      return null;
    }
  }

  @Override
  public AbstractNode get() {
    String topComponent = pathHelper.getComponents().get(0);

    Class<? extends AbstractNode> nodeClass = getComponentClass(topComponent);
    try {
      return injector.createChildInjector(new AbstractModule() {
        @Override
        protected void configure() {
          bind(UrlPath.class).toInstance(pathHelper);
          install(new CTFPluginNodesModule(config));
        }
      }).getInstance(nodeClass);
    } catch (Exception e) {
      log.error("Cannot instantiate node class " + nodeClass
          + " for component " + topComponent, e);
      return null;
    }  }
}
