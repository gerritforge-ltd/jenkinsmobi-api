package mobi.jenkinsci.server.realm;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.Before;
import org.junit.Test;

import static mobi.jenkinsci.test.InjectorMatcher.hasInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RealmModuleTest {

  private Injector injector;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new RealmModule());
  }

  @Test
  public void accountRegistryShouldBeBound() {
    assertThat(injector, hasInstance(AccountRegistry.class));
  }

  @Test
  public void loginServiceShouldBeBound() {
    assertThat(injector, hasInstance(LoginService.class));
  }

  @Test
  public void identityServiceShouldBeBound() {
    assertThat(injector, hasInstance(IdentityService.class));
  }

  @Test
  public void authenticatorShouldBeBound() {
    assertThat(injector, hasInstance(Authenticator.class));
  }

  @Test
  public void accessRestrictionsThroughAccessServiceShouldBeBoundToNotEmptyList() {
    assertThat(injector, hasInstance(AccessService.class,
            hasProperty("accessConstraints", not(emptyCollectionOf(Constraint.class)))));
  }
}
