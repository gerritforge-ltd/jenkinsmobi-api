package mobi.jenkinsci.server;

import com.google.inject.*;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import lombok.extern.slf4j.Slf4j;
import mobi.jenkinsci.server.realm.AccessService;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

@Slf4j
public class GuiceServer {

  private static final int DEFAULT_HTTP_PORT = 8080;
  private final int jettyServerPort;
  private final ServletModule servletModule;
  private Server jettyServer;
  private boolean started;
  private Injector injector;

  public GuiceServer(int serverPort, ServletModule servletModule) {
    this.jettyServerPort = serverPort;
    this.servletModule = servletModule;
  }

  public GuiceServer(ServletModule servletModules) {
    this(DEFAULT_HTTP_PORT, servletModules);
  }

  public Injector initInjector(Module... extraModules) {
    Module[] modules = new Module[extraModules.length + 1];
    modules[0] = servletModule;
    System.arraycopy(extraModules, 0, modules, 1, extraModules.length);
    injector = Guice.createInjector(modules);
    return injector;
  }


  public Injector getInjector() {
    if (injector == null) {
      return initInjector();
    } else {
      return injector;
    }
  }

  public void start() throws Exception {
    if (!started) {
      jettyServer = getJettyServer(getInjector());
      jettyServer.start();
      started = true;
    }
  }

  public void stop() throws Exception {
    if (started) {
      jettyServer.stop();
      jettyServer = null;
      started = false;
    }
  }

  private Server getJettyServer(final Injector servletInjector) {
    Server jettyServer = new Server(jettyServerPort);
    final ServletContextHandler app = new ServletContextHandler();
    GuiceFilter filter = servletInjector.getInstance(GuiceFilter.class);
    app.addFilter(new FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
    app.addEventListener(new GuiceServletContextListener() {
      @Override
      protected Injector getInjector() {
        return servletInjector;
      }
    });

    // This servlet is never used but apparently Jetty requires at least one Servlet to run
    final ServletHolder ds = app.addServlet(DefaultServlet.class, "/");
    ds.setInitParameter("dirAllowed", "false");
    ds.setInitParameter("redirectWelcome", "false");
    ds.setInitParameter("useFileMappedBuffer", "false");
    ds.setInitParameter("gzip", "true");

    app.setSecurityHandler(getConfiguredSecurityHandler(app));

    jettyServer.setHandler(app);
    return jettyServer;
  }

  private ConstraintSecurityHandler getConfiguredSecurityHandler(ServletContextHandler app) {
    try {
      ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();

      securityHandler.setLoginService(getInstance(LoginService.class));
      securityHandler.setIdentityService(getInstance(IdentityService.class));

      for (ConstraintMapping constraint : getInstance(AccessService.class).getAccessConstraints()) {
        securityHandler.addConstraintMapping(constraint);
      }

      Authenticator authenticator = getInstance(Authenticator.class);
      authenticator.setConfiguration(securityHandler);
      securityHandler.setAuthenticator(authenticator);

      return securityHandler;

    } catch (ConfigurationException | ProvisionException e) {
      GuiceServer.log.debug("No LoginService configured in Guice bindings: " + e.getMessage());
      return null;
    }
  }

  public <T> T getInstance(Class<T> clazz) {
    return getInjector().getInstance(clazz);
  }
}
