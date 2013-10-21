package mobi.jenkinsci.server.core.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import mobi.jenkinsci.plugin.PluginLoader;

import com.google.inject.Inject;

public class PluginLoaderServlet extends HttpServlet {
  private static final long serialVersionUID = -187533700964158022L;
  
  @Inject
  private PluginLoader pluginLoader;
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    pluginLoader.loadPlugins();
  }
}
