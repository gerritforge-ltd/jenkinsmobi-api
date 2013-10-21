package mobi.jenkinsci.server.core.module;

import com.google.inject.servlet.ServletModule;
import mobi.jenkinsci.server.core.net.NetModule;
import mobi.jenkinsci.server.core.services.ServicesModule;
import mobi.jenkinsci.server.core.servlet.HttpRequestDispatcherServlet;
import mobi.jenkinsci.server.core.servlet.PluginConfigServlet;
import mobi.jenkinsci.server.core.servlet.PluginLoaderFilter;
import mobi.jenkinsci.server.realm.RealmModule;

public class PluginsServletContainerModule extends ServletModule {

  @Override
  protected void configureServlets() {
    install(new RealmModule());

    bind(String.class).annotatedWith(CanonicalWebUrl.class).toInstance(System.getProperty("canonicalUrl", ""));

    filter("/*").through(PluginLoaderFilter.class);

    serve("/config").with(PluginConfigServlet.class);
    serve("/*").with(HttpRequestDispatcherServlet.class);

    // FIXME: define all the other bindings
  }
}
