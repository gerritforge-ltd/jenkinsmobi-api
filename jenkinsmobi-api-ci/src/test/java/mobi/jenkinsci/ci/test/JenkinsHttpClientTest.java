package mobi.jenkinsci.ci.test;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import javax.security.auth.login.LoginException;

import mobi.jenkinsci.ci.client.JenkinsConfig;
import mobi.jenkinsci.ci.client.JenkinsHttpClient;
import mobi.jenkinsci.plugin.PluginConfig;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JenkinsHttpClientTest {

  private JenkinsConfig config;
  private JenkinsHttpClient http;
  private PluginConfig pluginConfig;

  @Before
  public void setUp() throws MalformedURLException {
    config = new JenkinsConfig(pluginConfig);
    config.setUrl(JenkinsTestConstants.JENKINS_URL);
    config.setUsername(JenkinsTestConstants.JENKINS_USERNAME);
    config.setPassword(JenkinsTestConstants.JENKINS_PASSWORD);
    http = new JenkinsHttpClient(config);
  }

  @Test
  public void testGetJenkinsHomePageReturnsHttpOk() throws Exception {
    HttpGet get = new HttpGet(config.getUrl());
    HttpResponse response = http.execute(get);
    Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusLine()
        .getStatusCode());
  }

  @Test(expected = LoginException.class)
  public void testGetJenkinsHomePageWithInvalidCredentialsReturnsSsoSpecificError() throws Exception {
    config.setPassword(JenkinsTestConstants.JENKINS_PASSWORD + "_wrong");
    http = new JenkinsHttpClient(config);
    http.execute(new HttpGet(config.getUrl()));
  }
}
