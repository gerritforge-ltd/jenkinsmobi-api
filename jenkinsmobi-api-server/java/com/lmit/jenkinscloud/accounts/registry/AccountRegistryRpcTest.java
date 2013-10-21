package com.lmit.jenkinscloud.accounts.registry;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lmit.jenkinscloud.commons.JenkinsCloudAccount;
import com.lmit.jenkinscloud.commons.JenkinsCloudPlugin;
import com.lmit.jenkinscloud.commons.PathHelper;

public class AccountRegistryRpcTest {
  public static final String TEST_SUBSCRIBER_ID = "1234567890";
  private static final String TEST_PLUGIN_NAME_ADDED = "new-plugin-added";

  @BeforeClass
  public static void setUp() throws IOException {
    Configuration config = Configuration.getInstance();
    PathHelper.getFile("subscribers.properties.test").delete();
    config.setStorageSubscribers("subscribers.properties.test");
    PathHelper.getFile("pluginstore.test").delete();
    config.setStoragePlugins("pluginstore.test");
    PathHelper.getFile("accountstore.test").delete();
    config.setStorageAccounts("accountstore.test");
  }

  @Test
  public void testNewSelfRegisteredSubscriberIsNotNull() throws Exception {
    assertNotNull(getNewSelfRegisteredAccount());
  }

  private JenkinsCloudAccount getNewSelfRegisteredAccount() throws IOException {
    return getRegisteredAccount(getNewSubscriberId());
  }

  private JenkinsCloudAccount getRegisteredAccount(String subscriberId)
      throws IOException {
    return AccountRegistry.getInstance().getAccountBySubscriberId(
        subscriberId);
  }

  @Test
  public void testNewSelfRegisteredSubscriberHasValidUsername()
      throws Exception {
    String subscriberId = getNewSubscriberId();
    assertEquals(subscriberId, getRegisteredAccount(subscriberId).username);
  }

  @Test
  public void testNewSelfRegisteredSubscriberHasAtLeastOnePlugin()
      throws Exception {
    assertNotNull(getNewSelfRegisteredAccount().getPlugins().size() >= 1);
  }

  @Test
  public void testExistingSubscriberIsNotNull() throws Exception {
    getRegisteredAccount(TEST_SUBSCRIBER_ID);
    assertNotNull(getRegisteredAccount(TEST_SUBSCRIBER_ID));
  }

  @Test
  public void testExistingSubscriberHasValidUsername() throws Exception {
    getRegisteredAccount(TEST_SUBSCRIBER_ID);
    assertEquals(TEST_SUBSCRIBER_ID, getRegisteredAccount(TEST_SUBSCRIBER_ID).username);
  }

  @Test
  public void testExistingSubscriberHasAtLeastOnePlugin() throws Exception {
    getRegisteredAccount(TEST_SUBSCRIBER_ID);
    assertTrue(getRegisteredAccount(TEST_SUBSCRIBER_ID).getPlugins().size() >= 1);
  }
  
  @Test 
  public void testAddingPluginToSubscriberReturnsLargerPluginList() throws Exception {
    JenkinsCloudAccount account = getRegisteredAccount(TEST_SUBSCRIBER_ID);
    account.addPlugin(new JenkinsCloudPlugin(TEST_PLUGIN_NAME_ADDED));
    JenkinsCloudAccount newAccount = AccountRegistry.getInstance().saveAccountToSubscribers(account);
    assertNotNull(newAccount);
    assertEquals(account.getPlugins().size(), newAccount.getPlugins().size());
    boolean pluginFound = false;
    for (JenkinsCloudPlugin plugin : newAccount.getPlugins()) {
      if(plugin.name.equals(TEST_PLUGIN_NAME_ADDED)) {
        pluginFound = true;
      }
    }
    assertTrue(pluginFound);
  }

  private String getNewSubscriberId() {
    try {
      Thread.sleep(1L);
    } catch (InterruptedException e) {
    }
    return "" + System.currentTimeMillis();
  }
}
