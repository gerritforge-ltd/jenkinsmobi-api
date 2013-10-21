package mobi.jenkinsci.ci.test;

import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_JOB;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_PASSWORD;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_URL;
import static mobi.jenkinsci.ci.test.JenkinsTestConstants.JENKINS_USERNAME;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import mobi.jenkinsci.ci.JenkinsCIPlugin;
import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.model.ItemNode;
import mobi.jenkinsci.net.UrlPath;
import mobi.jenkinsci.plugin.PluginConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsCIPluginTest {
  private static final int JENKINSCI_NUM_ENTRYPOINTS = 3;

  private JenkinsCIPlugin jenkinsci;
  private PluginConfig jenkinsciConf;

  @Mock
  private Account account;

  @Before
  public void setUp() throws Exception {
    jenkinsci = new JenkinsCIPlugin();
    jenkinsci.configure(new Properties());
    jenkinsciConf = new PluginConfig("jenkins", jenkinsci.getType());
    jenkinsciConf.setUrl(JENKINS_URL);
    jenkinsciConf.setUsername(JENKINS_USERNAME);
    jenkinsciConf.setPassword(JENKINS_PASSWORD);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testHasAllEntryPoints() throws Exception {
    final List<ItemNode> entryPoints = jenkinsci.getEntryPoints(jenkinsciConf);
    Assert.assertNotNull(entryPoints);
    Assert.assertEquals(JENKINSCI_NUM_ENTRYPOINTS, entryPoints.size());
  }

  @Test
  public void testHasViewsNode() throws Exception {
    assertNotNull(findEntryPoint("views"));
  }

  @Test
  public void testHashAllViewsNode() throws Exception {
    assertNotNull(findItemNode("all", findViewsNode().getPayload()));
  }

  private ItemNode findViewsNode() throws Exception {
    final ItemNode views = findEntryPoint("views");
    return jenkinsci.getNode(account, jenkinsciConf, new UrlPath(views.getPath()));
  }

  @Test
  public void testHasTestJobNode() throws Exception {
    assertNotNull(findJob());
  }

  @Test
  public void testJobNodeHasTitle() throws Exception {
    assertNotNull(findJob().getTitle());
  }

  @Test
  public void testJobNodeHasPath() throws Exception {
    assertNotNull(findJob().getPath());
  }

  @Test
  public void testJobNodeHasViewTitle() throws Exception {
    assertNotNull(findJob().getViewTitle());
  }

  private ItemNode findJob() throws UnsupportedEncodingException, Exception {
    final ItemNode allViewsNode = findAllViewsNode();
    final ItemNode jobNode = findItemNode(JENKINS_JOB, allViewsNode.getPayload());
    return jenkinsci.getNode(
        account,
        jenkinsciConf,
        new UrlPath(String.format("/views/%s/%s", allViewsNode.getPath(),
            jobNode.getPath())));
  }

  private ItemNode findAllViewsNode() throws Exception {
    return jenkinsci.getNode(account, jenkinsciConf, new UrlPath(findViewsNode()
        .getPath(), "All"));
  }

  private ItemNode findEntryPoint(final String suffix) throws Exception {
    final List<ItemNode> entryPoints = jenkinsci.getEntryPoints(jenkinsciConf);
    return findItemNode(suffix, entryPoints);
  }

  private ItemNode findItemNode(String suffix, final List<ItemNode> nodes) {
    if (nodes == null) {
      return null;
    }
    suffix = suffix.toLowerCase();

    ItemNode nodeFound = null;
    for (int i = 0; i < nodes.size() && nodeFound == null; i++) {
      final ItemNode subNode = nodes.get(i);
      if ((subNode.getPath() != null
          && subNode.getPath().toLowerCase().endsWith(suffix) || (subNode
          .getTitle() != null && subNode.getTitle().equalsIgnoreCase(suffix)))) {
        nodeFound = subNode;
      }
    }

    return nodeFound;
  }

  @Test
  public void testJobHasArtifacts() throws Exception {
    final ItemNode artifacts = findJobDetails("artifacts");
    assertNotNull(artifacts);
  }

  @Test
  public void testJobArtifactsHasTitle() throws Exception {
    assertNotNull(findJobDetails("artifacts").getTitle());
  }

  @Test
  public void testJobArtifactsHasViewTitle() throws Exception {
    assertNotNull(findJobDetails("artifacts").getViewTitle());
  }

  @Test
  public void testJobArtifactsHasPayload() throws Exception {
    assertNotNull(findJobDetails("artifacts").getPayload().size() > 0);
  }

  @Test
  public void testJobHasChanges() throws Exception {
    final ItemNode artifacts = findJobDetails("changes");
    assertNotNull(artifacts);
  }

  @Test
  public void testJobChangesHasTitle() throws Exception {
    assertNotNull(findJobDetails("changes").getTitle());
  }

  @Test
  public void testJobChangesHasViewTitle() throws Exception {
    assertNotNull(findJobDetails("changes").getViewTitle());
  }

  @Test
  public void testJobChangesHasPayload() throws Exception {
    assertNotNull(findJobDetails("changes").getPayload().size() > 0);
  }

  @Test
  public void testJobHasModules() throws Exception {
    final ItemNode artifacts = findJobDetails("modules");
    assertNotNull(artifacts);
  }

  @Test
  public void testJobModulesHasTitle() throws Exception {
    assertNotNull(findJobDetails("modules").getTitle());
  }

  @Test
  public void testJobModulesHasViewTitle() throws Exception {
    assertNotNull(findJobDetails("modules").getViewTitle());
  }

  @Test
  public void testJobModulesHasPayload() throws Exception {
    assertNotNull(findJobDetails("modules").getPayload().size() > 0);
  }

  private ItemNode findJobDetails(final String detail) throws Exception,
      UnsupportedEncodingException, URISyntaxException {
    final ItemNode allViews = findAllViewsNode();
    final ItemNode job = findJob();
    final ItemNode artifacts =
        jenkinsci.getNode(account, jenkinsciConf,
            new UrlPath("views/" + allViews.getPath() + "/" + job.getPath(),
                "/" + detail));
    return artifacts;
  }

}
