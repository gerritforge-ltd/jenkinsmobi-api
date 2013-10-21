package mobi.jenkinsci.server.realm;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.guice.DynamicList;
import mobi.jenkinsci.server.core.services.ServicesModule;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;

public class RealmModule extends AbstractModule {
  @Override
  protected void configure() {

    install(new ServicesModule());

    bind(AccountRegistry.class).to(IniAccountRegistry.class);

    install(new FactoryModuleBuilder().implement(Account.class, Account.class).build(Account.Factory.class));

    bind(LoginService.class).to(JenkinsMobiLoginService.class);
    bind(IdentityService.class).toInstance(new DefaultIdentityService());
    bind(Authenticator.class).toInstance(new BasicAuthenticator());

    DynamicList.listOf(binder(), AccessRestriction.class);
    DynamicList.bind(binder(), AccessRestriction.class)
            .toInstance(new AccessRestriction("/*", Constraint.__BASIC_AUTH, new String[]{"user"}));
  }
}
