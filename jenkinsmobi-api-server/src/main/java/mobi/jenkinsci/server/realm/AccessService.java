package mobi.jenkinsci.server.realm;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import mobi.jenkinsci.guice.DynamicList;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Constraint;

import java.util.List;

public class AccessService {

  private final DynamicList<AccessRestriction> restrictions;

  @Inject
  public AccessService(DynamicList<AccessRestriction> restrictions) {
    this.restrictions = restrictions;
  }

  public List<ConstraintMapping> getAccessConstraints() {
    List<ConstraintMapping> accessConstraints = Lists.newArrayList();

    for (AccessRestriction restriction: restrictions) {
      accessConstraints.add(getConstraint(restriction));
    }

    return accessConstraints;
  }

  private ConstraintMapping getConstraint(AccessRestriction restriction) {
    Constraint constraint = new Constraint();
    constraint.setName(restriction.getName());
    constraint.setRoles(restriction.getRoles());
    constraint.setAuthenticate(true);

    ConstraintMapping constraintMapping = new ConstraintMapping();
    constraintMapping.setConstraint(constraint);
    constraintMapping.setPathSpec(restriction.getPathSpec());

    return constraintMapping;
  }
}
