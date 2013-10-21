package mobi.jenkinsci.server.realm;

import com.google.common.collect.Sets;
import lombok.Getter;
import mobi.jenkinsci.commons.Account;
import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import java.util.Collections;

public class JenkinsMobiIdentity implements UserIdentity {

  @Getter
  private final Account userPrincipal;

  @Getter
  private final Subject subject;

  JenkinsMobiIdentity(Account userAccount) {
    this.userPrincipal = userAccount;
    this.subject = new Subject(true, Sets.newHashSet(userAccount), Collections.emptySet(), Collections.emptySet());
  }

  @Override
  public boolean isUserInRole(String role, Scope scope) {
    return userPrincipal.getRoles().contains(role);
  }
}
