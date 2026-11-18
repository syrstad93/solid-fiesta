package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests on the {@link LinkedClients} class.
 */
public class LinkedClientsTest {

  /**
   * Reset all singleton instances to null after each test
   * to ensure a clean state for the next test.
   *
   * @throws IOException if an I/O error occurs when closing the server socket.
   */
  @AfterEach
  public void tearDown() throws IOException {
    Server server = Server.getInstance();
    server.closeServer();
    ClientHandlerList chl = ClientHandlerList.getInstance();
    chl.resetInstance();
    RoomList rl = RoomList.getInstance();
    rl.resetInstance();
    ClientStatusMap csm = ClientStatusMap.getInstance();
    csm.resetInstance();
    LinkedClients lc = LinkedClients.getInstance();
    lc.resetInstance();
  }

  @Test
  public void checkSingletonInstance() {
    LinkedClients instance1 = LinkedClients.getInstance();
    LinkedClients instance2 = LinkedClients.getInstance();
    assertSame(instance1, instance2);
  }

  @Test
  public void checkSingletonInstanceNotSameAfterReset() {
    LinkedClients instance1 = LinkedClients.getInstance();
    instance1.resetInstance();
    LinkedClients instance2 = LinkedClients.getInstance();
    assertNotSame(instance1, instance2);
  }

  @Test
  public void addSource_checkAddedWithEmptyListOfLinks() throws IOException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler = new ClientHandler(socket, 1, server);

    linkedClients.addSource(clientHandler);

    assertEquals(0, linkedClients.getNumberOfLinks(clientHandler));
    assertEquals(0, linkedClients.getNumberOfLinks());
  }

  @Test
  public void addLinkForSourcePreviouslyAddedAndCheckNumberOfLinks() throws IOException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    // Add another link for the same source
    Socket socket3 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler3 = new ClientHandler(socket3, 3, server);

    linkedClients.addLink(clientHandler1, clientHandler3, 20, "TEST2");

    assertEquals(2, linkedClients.getNumberOfLinks(clientHandler1));
  }

  @Test
  public void addLinkForSourceNotPreviouslyAddedAndTriggerCommandToBeSent()
      throws IOException, InterruptedException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    ClientStatusMap statusMap = ClientStatusMap.getInstance();
    statusMap.put(clientHandler1, 10);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    Thread.sleep(200); // Wait for the command to be sent

    String sentMessage = clientHandler2.getLastSentMessage();
    assertEquals("CH02/CH01/TEST/=", sentMessage);
    assertTrue(linkedClients.linkIsTriggered(clientHandler1, 0));
  }

  @Test
  public void addTwoLinksWithDifferentTriggerValuesAndCheckTriggering()
      throws IOException, InterruptedException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    // Add another link for the same source with a different trigger value
    Socket socket3 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler3 = new ClientHandler(socket3, 3, server);

    linkedClients.addLink(clientHandler1, clientHandler3, 20, "TEST2");

    assertEquals(2, linkedClients.getNumberOfLinks(clientHandler1));

    ClientStatusMap statusMap = ClientStatusMap.getInstance();
    statusMap.put(clientHandler1, 10);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    Thread.sleep(200); // Wait for the command to be sent

    String sentMessageToClient2 = clientHandler2.getLastSentMessage();
    String sentMessageToClient3 = clientHandler3.getLastSentMessage();

    assertEquals("CH02/CH01/TEST/=", sentMessageToClient2);
    assertNull(sentMessageToClient3);
  }

  @Test
  public void triggerCommandToBeSent_updateStatusAgainAboveTriggerValueAndCheckCommandNotSentAgain()
      throws IOException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    ClientStatusMap statusMap = ClientStatusMap.getInstance();
    statusMap.put(clientHandler1, 10);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    String lastSentMessageToClient2 = clientHandler2.getLastSentMessage();
    assertEquals("CH02/CH01/TEST/=", lastSentMessageToClient2);

    linkedClients.editCommand(clientHandler1, 0, "TEST_UPDATED");

    statusMap.put(clientHandler1, 15);
    assertEquals("CH02/CH01/TEST/=", lastSentMessageToClient2); // Should be the same.
  }

  @Test
  public void afterTriggeredAndStatusDropsBelowTriggerValue_checkLinkIsTriggeredReturnsFalse()
      throws IOException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    ClientStatusMap statusMap = ClientStatusMap.getInstance();
    statusMap.put(clientHandler1, 10);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    String lastSentMessageToClient2 = clientHandler2.getLastSentMessage();
    assertEquals("CH02/CH01/TEST/=", lastSentMessageToClient2);

    statusMap.put(clientHandler1, 5);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    assertFalse(linkedClients.linkIsTriggered(clientHandler1, 0));
  }

  @Test
  public void addLinkAndCheckEditMethods() throws IOException, InterruptedException {
    LinkedClients linkedClients = LinkedClients.getInstance();

    Server server = Server.getInstance();
    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler1 = new ClientHandler(socket1, 1, server);

    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler2 = new ClientHandler(socket2, 2, server);

    Socket socket3 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler3 = new ClientHandler(socket3, 3, server);

    linkedClients.addLink(clientHandler1, clientHandler2, 10, "TEST");

    assertEquals(1, linkedClients.getNumberOfLinks(clientHandler1));

    linkedClients.editCommand(clientHandler1, 0, "TEST_UPDATED");
    linkedClients.editTrigger(clientHandler1, 0, 20);
    linkedClients.editDestination(clientHandler1, 0, clientHandler3);

    ClientStatusMap statusMap = ClientStatusMap.getInstance();
    statusMap.put(clientHandler1, 20);

    linkedClients.checkLinkTriggersAndSendCommandIfTriggered(clientHandler1);

    Thread.sleep(200); // Wait for the command to be sent

    String sentMessage = clientHandler3.getLastSentMessage();
    assertEquals("CH03/CH01/TEST_UPDATED/=", sentMessage);
    assertTrue(linkedClients.linkIsTriggered(clientHandler1, 0));
  }
}
