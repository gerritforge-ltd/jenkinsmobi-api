package mobi.jenkinsci.alm.assembla.client.test;

import static mobi.jenkinsci.alm.assembla.client.test.TestConstants.TEST_APP_ID;
import static mobi.jenkinsci.alm.assembla.client.test.TestConstants.TEST_APP_SECRET;
import static mobi.jenkinsci.alm.assembla.client.test.TestConstants.TEST_PASSWORD;
import static mobi.jenkinsci.alm.assembla.client.test.TestConstants.TEST_USERNAME;
import static mobi.jenkinsci.alm.assembla.client.test.TestConstants.TICKET_SAMPLE_COMMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import mobi.jenkinsci.alm.assembla.client.AssemblaClient;
import mobi.jenkinsci.alm.assembla.objects.AssemblaMilestone;
import mobi.jenkinsci.alm.assembla.objects.AssemblaMilestones;
import mobi.jenkinsci.alm.assembla.objects.AssemblaSpace;
import mobi.jenkinsci.alm.assembla.objects.AssemblaSpaces;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTicket;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTicketComment;
import mobi.jenkinsci.alm.assembla.objects.AssemblaTickets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAssemblaClient {

  private AssemblaClient c;

  @Before
  public void setUp() {
    c =
        new AssemblaClient(TEST_APP_ID, TEST_APP_SECRET, TEST_USERNAME,
            TEST_PASSWORD);
  }
  
  @Test
  public void testGetSpacesNotNullOrEmptyAfterLoginRefresh() throws Exception {
    testGetSpacesNotNullOrEmpty();
    c.loginRefresh();
    testGetSpacesNotNullOrEmpty();
  }

  @Test
  public void testGetSpacesNotNullOrEmpty() throws Exception {
    AssemblaSpaces spaces = c.spaces();
    assertNotNull(spaces);
    Assert.assertNotNull(spaces.items);
    Assert.assertTrue(spaces.items.size() > 0);
    for (AssemblaSpace space : spaces.items) {
      Assert.assertNotNull(space.id);
      Assert.assertNotNull(space.name);
    }
  }

  @Test
  public void testGetTicketsFromAllSpacesAreNotNullAndNotEmpty()
      throws Exception {
    for (AssemblaSpace space : c.spaces().items) {
      AssemblaTickets tickets = c.getTickets(space.id);
      assertNotNull(tickets);
      assertNotNull(tickets.items);

      for (AssemblaTicket ticket : tickets.items) {
        assertNotNull(ticket);
        assertNotNull(ticket.id);
        assertTrue(ticket.number > 0);
      }
    }
  }

  @Test
  public void testGetMilestones() throws Exception {
    for (AssemblaSpace space : c.spaces().items) {
      AssemblaMilestones milestones = c.getMilestones(space.id);
      for (AssemblaMilestone milestone : milestones.items) {
        assertNotNull(milestone);
        assertNotNull(milestone.title);
      }
    }
  }

  @Test
  public void testCanAddCommentToFirstTicketInFistSpace() throws Exception {
    for (AssemblaSpace space : c.spaces().items) {
      AssemblaTickets tickets = c.getTickets(space.id);
      assertNotNull(tickets);
      assertNotNull(tickets.items);
      if (tickets.items.size() > 1) {
        AssemblaTicketComment comment =
            c.addComment(space.id, tickets.items.get(0).number,
                TICKET_SAMPLE_COMMENT);
        assertNotNull(comment);
        assertEquals(TICKET_SAMPLE_COMMENT, comment.comment);
      }
    }
  }
}
