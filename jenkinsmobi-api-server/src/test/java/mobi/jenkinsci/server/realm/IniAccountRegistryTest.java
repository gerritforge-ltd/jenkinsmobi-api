package mobi.jenkinsci.server.realm;

import com.google.inject.assistedinject.Assisted;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.plugin.PluginConfig;
import mobi.jenkinsci.server.Config;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IniAccountRegistryTest {

  private static final String TEST_USER = "testuser";

  @Mock
  private Config config;

  private Account.Factory accountFactory = new Account.Factory() {
    @Override
    public Account get(@Assisted String name, @Assisted Set<String> roles) {
      return new Account(name, roles, null);
    }
  };

  private IniAccountRegistry accountRegistry;

  private Ini accountIni;

  private File accountIniFile;

  @Before
  public void setUp() throws IOException {
    accountIniFile = File.createTempFile("accounts", "ini");
    accountIni = new Ini(accountIniFile);
    when(config.loadIni(anyString())).thenReturn(accountIni);
    when(config.loadIni(anyString(), anyBoolean())).thenReturn(accountIni);
    when(config.getFile(eq(Config.SUBSCRIBERS_CONFIG))).thenReturn(accountIniFile);

    accountRegistry = new IniAccountRegistry(config, accountFactory);
  }

  @Test
  public void getShouldReturnAccountWhenIniSectionOfAccountNameIsDefined() throws Exception {
    accountIni.add(TEST_USER);

    assertThat(accountRegistry.get(TEST_USER), notNullValue(Account.class));
  }

  @Test
  public void getShouldReturnAccountWithRolesWhenIniSectionOfAccountNameContainsRoles() throws Exception {
    Profile.Section accountSection = accountIni.add(TEST_USER);
    accountSection.add("roles", "user,administrator");

    assertThat(accountRegistry.get(TEST_USER), hasProperty("roles", contains("user", "administrator")));
  }

  @Test
  public void getShouldReturnAccountWithPluginsWhenIniSectionOfAccountNameContainsPluginsSection() throws Exception {
    accountIni.add(TEST_USER).addChild("plugins").addChild("myPlugin").add("type", "pluginType");

    assertThat(accountRegistry.get(TEST_USER).getPluginConfig("myPlugin"), hasProperty("type", equalTo("pluginType")));
  }

  @Test
  public void getShouldReturnNullWhenAccountIniSectionDoesNotExist() throws Exception {
    assertThat(accountRegistry.get(TEST_USER), nullValue(Account.class));
  }

  @Test
  public void addUserShouldCreateNewSectionWithUsernameInIni() throws Exception {
    Account newAccount = new Account(TEST_USER, new HashSet<String>(), null);

    accountRegistry.add(newAccount);

    assertThat(accountIni.get(TEST_USER), notNullValue(Profile.Section.class));
  }

  @Test
  public void addUserShouldSaveIniToFilesystem() throws Exception {
    Account newAccount = new Account(TEST_USER, new HashSet<String>(), null);
    accountRegistry.add(newAccount);

    Ini savedIniFile = new Ini(accountIniFile);
    assertThat(savedIniFile.get(TEST_USER), notNullValue(Profile.Section.class));
  }

  @Test
  public void getUnknownAccountBySubscriberIdShouldAutomaticallyCreateNewAccount() throws Exception {
    assertThat(accountRegistry.getAccountBySubscriberId(TEST_USER), notNullValue(Account.class));

    assertThat(accountIni.get(TEST_USER), notNullValue(Profile.Section.class));
  }

  @Test
  public void updateShouldSaveAccountPlugins() throws Exception {
    accountIni.add(TEST_USER);
    Account account = accountRegistry.get(TEST_USER);
    account.addPlugin(new PluginConfig("newPlugin", "newPluginType"));

    accountRegistry.update(account);

    Ini savedIniFile = new Ini(accountIniFile);
    final Profile.Section userSection = savedIniFile.get(TEST_USER);
    assertThat(userSection, notNullValue());
    final Profile.Section pluginsSection = userSection.getChild("plugins");
    assertThat(pluginsSection, notNullValue());
    final Profile.Section newPluginSection = pluginsSection.getChild("newPlugin");
    assertThat(newPluginSection, notNullValue());
    assertThat(newPluginSection.get("type"), equalTo("newPluginType"));
  }
}
