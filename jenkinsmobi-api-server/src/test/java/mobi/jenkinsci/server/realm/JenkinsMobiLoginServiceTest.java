package mobi.jenkinsci.server.realm;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.server.Config;
import org.eclipse.jetty.server.UserIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsMobiLoginServiceTest {

  private static final String TEST_USERNAME = "myusername";
  private static final String SHARED_CREDENTIALS = "sharedSecret123_!";
  @Mock
  private Config config;

  @Mock
  private AccountRegistry registry;

  @InjectMocks
  private JenkinsMobiLoginService loginService;

  @Test
  public void getNameShouldReturnJenkinsMobiLoginServiceName() throws Exception {
    assertThat(loginService, hasProperty("name", equalTo(JenkinsMobiLoginService.JENKINSMOBI_API_NAME)));
  }

  @Test
  public void loginShouldSucceedWhenAccountIsInRegistryAndCredentialsIsEqualToSharedSecretInConfig() throws Exception {
    when(registry.get(TEST_USERNAME)).thenReturn(new Account(TEST_USERNAME, new HashSet<String>(), null));
    when(config.getJenkinsCloudSecret()).thenReturn(SHARED_CREDENTIALS);

    assertThat(loginService.login(TEST_USERNAME, SHARED_CREDENTIALS), notNullValue(UserIdentity.class));
  }

  @Test
  public void loginShouldFailWhenAccountIsNotInRegistry() throws
          Exception {
    when(config.getJenkinsCloudSecret()).thenReturn(SHARED_CREDENTIALS);

    assertThat(loginService.login(TEST_USERNAME, SHARED_CREDENTIALS), nullValue(UserIdentity.class));
  }

  @Test
  public void loginShouldFailWithAnInvalidSecret() throws
          Exception {
    when(registry.get(TEST_USERNAME)).thenReturn(new Account(TEST_USERNAME, new HashSet<String>(), null));
    when(config.getJenkinsCloudSecret()).thenReturn(SHARED_CREDENTIALS);

    assertThat(loginService.login(TEST_USERNAME, "anInvalidSecret"), nullValue(UserIdentity.class));
  }
}
