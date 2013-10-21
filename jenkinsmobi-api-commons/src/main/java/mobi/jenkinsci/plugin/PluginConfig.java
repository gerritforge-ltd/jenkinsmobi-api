// Copyright (C) 2013 GerritForge www.gerritforge.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package mobi.jenkinsci.plugin;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

public class PluginConfig {

  @EqualsAndHashCode
  public static class Key {
    @Getter
    private final String name;
    @Getter
    private final String type;

    public Key(final String name, final String type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String toString() {
      return "Key [name=" + name + ", type=" + type + "]";
    }
  }

  public static Key key(final String name, final String type) {
    final Key key = new Key(name, type);
    return key;
  }

  @Getter
  private final Key key;

  public String getName() {
    return key.getName();
  }

  public String getType() {
    return key.getType();
  }

  @Getter
  @Setter
  private String parentAccountUsername;
  @Getter
  @Setter
  private String description;
  @Getter
  @Setter
  private String url;
  @Getter
  @Setter
  private String username;
  @Getter
  @Setter
  private String password;
  @Getter
  @Setter
  private Map<String, String> options;

  public PluginConfig(final String name, final String type) {
    this.key = new Key(name, type);
  }
}
