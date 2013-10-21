package mobi.jenkinsci.ci.test;

import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_JOB;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_PASSWORD;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_URL;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_USERNAME;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.TEST_RETRY_COUNT;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.TEST_RETRY_SLEEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.List;

import mobi.jenkinsci.ci.client.JenkinsClient;
import mobi.jenkinsci.ci.client.JenkinsConfig;
import mobi.jenkinsci.ci.model.Artifact;
import mobi.jenkinsci.ci.model.Build;
import mobi.jenkinsci.ci.model.ChangeSet;
import mobi.jenkinsci.ci.model.ChangeSetItem;
import mobi.jenkinsci.ci.model.ComputerList;
import mobi.jenkinsci.ci.model.Job;
import mobi.jenkinsci.ci.model.JobArtifacts;
import mobi.jenkinsci.ci.model.JobName;
import mobi.jenkinsci.ci.model.Queue;
import mobi.jenkinsci.ci.model.ViewList;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.ResetNode;
import mobi.jenkinsci.plugin.PluginConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JenkinsClientTest {
  private JenkinsClient client;
  private JenkinsConfig config;
  private PluginConfig pluginConfig;
  private Account account;

  @Before
  public void setUp() throws Exception {
    pluginConfig = new PluginConfig("jenkins","jenkins");
    pluginConfig.setUrl(JENKINS_URL);
    pluginConfig.setUsername(JENKINS_USERNAME);
    pluginConfig.setPassword(JENKINS_PASSWORD);
    config = new JenkinsConfig(pluginConfig);
    client = new JenkinsClient(account, config);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetNotNullViewList() throws Exception {
    ViewList viewList = client.getViewList();
    assertNotNull(viewList);
    assertNotNull(viewList.getViews());
    assertNotNull(viewList.getPrimaryView());
  }

  @Test
  public void testGetNotNullComputerList() throws Exception {
    ComputerList computerList = client.getComputerList();
    assertNotNull(computerList);
  }

  @Test
  public void testGetNotNullQueue() throws Exception {
    Queue queue = client.getQueue();
    assertNotNull(queue);
  }

  @Test
  public void testCanTriggerBuildSuccessfully() throws Exception {
    JobName job = getJobNotBuilding();
    buildJob(job.path);
    assertTrue(client.getJob(job.path).isBuilding());
  }

  private Build buildJob(String jobPath) throws Exception,
      UnsupportedEncodingException, InterruptedException {
    Job job = null;
    AbstractNode result =
        client.execute(config.getUrl() + "/job/" + jobPath
            + "/build?delay=0sec");
    assertTrue(result instanceof ResetNode);
    for (int retryCount = 0; !(job = client.getJob(jobPath)).isBuilding()
        && retryCount < TEST_RETRY_COUNT; retryCount++) {
      Thread.sleep(TEST_RETRY_SLEEP);
    }

    return job.builds.get(0);
  }

  private Build buildJob() throws UnsupportedEncodingException,
      InterruptedException, Exception {
    return buildJob(JenkinsClient.urlEncode(JENKINS_JOB));
  }

  @Test
  public void testCanStopBuild() throws Exception {
    Job job = getJobNotBuilding();
    Build build = buildJob(job.path);
    AbstractNode result =
        client.execute(config.getUrl() + "/job/" + job.path
            + "/" + build.number + "/stop");
    assertTrue(result instanceof ResetNode);
    for (int retryCount = 0; (job = client.getJob(job.path)).isBuilding()
        && retryCount < TEST_RETRY_COUNT; retryCount++) {
      Thread.sleep(TEST_RETRY_SLEEP);
    }

    assertEquals("aborted", job.builds.get(0).result.toLowerCase());
  }

  private Job getJobNotBuilding() throws Exception {
    Job job = null;
    for (int retryCount = 0; (job =
        client.getJob(JenkinsClient.urlEncode(JENKINS_JOB))).isBuilding()
        && retryCount < TEST_RETRY_COUNT; retryCount++) {
      Thread.sleep(TEST_RETRY_SLEEP);
    }

    if (job.isBuilding()) {
      throw new Exception("No jobs found in Jenkins Server");
    }

    return job;
  }

  @Test
  public void testGetJobName() throws Exception {
    Job job = getJob();
    assertNotNull(job);
    assertEquals(JENKINS_JOB, job.name);
  }

  private Job getJob() throws Exception {
    Job job = client.getJob(JenkinsClient.urlEncode(JENKINS_JOB));
    return job;
  }

  @Test
  public void testGetJobPath() throws Exception {
    Job job = getJob();
    assertNotNull(job);
    assertEquals(JenkinsClient.urlEncode(JENKINS_JOB), job.path);
  }
  
  @Test
  public void testGetChangesContainIssues() throws Exception {
    Job job = getJob();
    Build jobBuild = job.getBuild("lastCompletedBuild");
    ChangeSet buildChanges =
        client.getJobChanges(JenkinsClient.urlEncode(JENKINS_JOB),
            jobBuild.number);
    assertNotNull(buildChanges);
    for (ChangeSetItem changeItem : buildChanges.items) {
      assertNotNull(changeItem.issue);
      
    }
  }

  @Test
  public void testGetNotEmptyJobArtifacts() throws Exception {
    assertNotNull(buildJob());
    Job job = getJob();
    JobArtifacts artifacts = (JobArtifacts) job.getDetail(Job.Detail.ARTIFACTS);
    assertNotNull(artifacts);
    List<Artifact> artifactList = artifacts.getArtifacts();
    assertNotNull(artifactList);
    assertTrue(artifactList.size() > 0);
  }
}
