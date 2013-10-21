package com.lmitsoftware.ctf;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lmitsoftware.ctf.model.NodesProvider;
import com.lmitsoftware.ctf.model.poc.PocNodesProvider;

public class CTFPluginModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(PocNodesProvider.Factory.class));
  }
}
