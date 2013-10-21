package mobi.jenkinsci.server.core.servlet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mobi.jenkinsci.server.core.services.PluginLoader;

import javax.servlet.*;
import java.io.IOException;

@Singleton
public class PluginLoaderFilter implements Filter {
  private static final long serialVersionUID = -187533700964158022L;

  private final PluginLoader pluginLoader;

  @Inject
  public PluginLoaderFilter(PluginLoader pluginLoader) {
    this.pluginLoader = pluginLoader;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    pluginLoader.loadPlugins();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
