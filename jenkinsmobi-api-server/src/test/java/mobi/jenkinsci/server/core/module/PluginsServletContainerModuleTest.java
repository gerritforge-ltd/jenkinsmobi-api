package mobi.jenkinsci.server.core.module;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import mobi.jenkinsci.net.HttpClientFactory;
import mobi.jenkinsci.server.core.servlet.HttpRequestDispatcherServlet;
import mobi.jenkinsci.server.core.servlet.PluginConfigServlet;
import mobi.jenkinsci.server.core.servlet.PluginLoaderFilter;
import org.junit.Before;
import org.junit.Test;

import static mobi.jenkinsci.test.InjectorMatcher.hasInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class PluginsServletContainerModuleTest {

  private Injector injector;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new PluginsServletContainerModule());
  }

  @Test
  public void canonicalWebUrlShouldBeBoundInGuiceModule() {
    assertThat(injector, hasInstance(Key.get(String.class, CanonicalWebUrl.class)));
  }

  @Test
  public void pluginLoaderFilterShouldBeBoundInGuiceModule() {
    assertThat(injector, hasInstance(PluginLoaderFilter.class));
  }

  @Test
  public void pluginConfigServletShouldBeBoundInGuiceModule() {
    assertThat(injector, hasInstance(PluginConfigServlet.class));
  }

  @Test
  public void httpRequestDispatcherShouldBeBoundInGuiceModule() {
    assertThat(injector, hasInstance(HttpRequestDispatcherServlet.class));
  }

  @Test
  public void httpClientFactoryShouldBeBoundInGuiceModule() {
    assertThat(injector, hasInstance(HttpClientFactory.class));
  }
}