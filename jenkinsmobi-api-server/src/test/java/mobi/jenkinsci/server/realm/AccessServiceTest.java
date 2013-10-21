package mobi.jenkinsci.server.realm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import mobi.jenkinsci.guice.DynamicList;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AccessServiceTest {

  @Test
  public void injectedAccessRestrictionsShouldBeReturnedAsListOfConstraints() throws Exception {
    AccessService accessService = getAccessServiceFromGuiceModule(new AbstractModule() {
      protected void configure() {
        DynamicList.listOf(binder(), AccessRestriction.class);
        DynamicList.bind(binder(), AccessRestriction.class).toInstance(new AccessRestriction("", "", new String[]{}));
        DynamicList.bind(binder(), AccessRestriction.class).toInstance(new AccessRestriction("", "", new String[]{}));
      }
    });

    assertThat(accessService, hasProperty("accessConstraints", contains(notNullValue(ConstraintMapping.class), notNullValue(ConstraintMapping.class))));
  }

  private AccessService getAccessServiceFromGuiceModule(Module guiceModule) {
    return Guice.createInjector(guiceModule).getInstance(AccessService.class);
  }

  @Test
  public void contraintMappingFieldsShouldMatchAccessRestriction() {
    final String path = "path";
    final String[] roles = {"role1", "role2"};

    AccessService accessService = getAccessServiceFromGuiceModule(new AbstractModule() {
      protected void configure() {
        DynamicList.listOf(binder(), AccessRestriction.class);
        DynamicList.bind(binder(), AccessRestriction.class).toInstance(new AccessRestriction(path, "", roles));
      }
    });

    ConstraintMapping mapping = accessService.getAccessConstraints().get(0);

    assertThat(mapping, allOf(hasProperty("pathSpec", equalTo(path)), hasProperty("constraint", notNullValue(Constraint.class))));
    assertThat(mapping.getConstraint(), allOf(hasProperty("authenticate", is(true)), hasProperty("roles", is(roles))));
  }
}
