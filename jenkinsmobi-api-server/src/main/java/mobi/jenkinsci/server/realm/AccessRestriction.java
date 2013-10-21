package mobi.jenkinsci.server.realm;

import lombok.Data;

@Data
public class AccessRestriction {
  private final String pathSpec;
  private final String name;
  private final String[] roles;
}
