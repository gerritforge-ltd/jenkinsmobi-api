package mobi.jenkinsci.server.core.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import mobi.jenkinsci.plugin.PluginFactory;
import org.junit.Test;

import static mobi.jenkinsci.test.InjectorMatcher.hasInstance;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ServicesModuleTest {

    @Test
    public void pluginRequestCommandShouldBeBound() {
        Injector testInjector = Guice.createInjector(new ServicesModule());

        assertThat(testInjector, hasInstance(PluginRequestCommand.class));
    }

  @Test
  public void pluginFactoryShouldBeBound() {
    Injector testInjector = Guice.createInjector(new ServicesModule());

    assertThat(testInjector, hasInstance(PluginFactory.class));
  }

}
