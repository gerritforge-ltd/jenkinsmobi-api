package mobi.jenkinsci.ci.test;

public interface JenkinsTestConstants {

  public static final String JENKINS_PASSWORD = System.getProperty(
      "jenkins.password", "bf333ma");
  public static final String JENKINS_USERNAME = System.getProperty(
      "jenkins.user", "admin");
  public static final String JENKINS_URL = System.getProperty("jenkins.url",
      "http://hudson-mobi.com/hudson");
  public static final String JENKINS_JOB = System.getProperty("jenkins.job",
      "Jenkins Job for Unit-Test");
  public static final int TEST_RETRY_COUNT = 15;
  public static final long TEST_RETRY_SLEEP = 1000L;
}
