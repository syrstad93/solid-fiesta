package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/***
 * Performs unit tests on the {@link ClientStatusMap} class.
 */
public class ClientStatusMapTest {

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
    ClientStatusMap instance1 = ClientStatusMap.getInstance();
    ClientStatusMap instance2 = ClientStatusMap.getInstance();
    assertSame(instance1, instance2);
  }

  @Test
  public void checkSingletonInstanceNotSameAfterReset() {
    ClientStatusMap instance1 = ClientStatusMap.getInstance();
    instance1.resetInstance();
    ClientStatusMap instance2 = ClientStatusMap.getInstance();
    assertNotSame(instance1, instance2);
  }

  @Test
  public void putAndUpdateStatus_checkGetStatusAndGetSizeAndContains() throws IOException {
    Server server = Server.getInstance();
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1, server);

    statusMap.put(clientHandler, 0);
    assert(statusMap.getStatus(clientHandler) == 0);

    statusMap.put(clientHandler, 1);
    assert(statusMap.getStatus(clientHandler) == 1);

    assertEquals(1, statusMap.getStatus(clientHandler));
    assertEquals(1, statusMap.size());
    assert(statusMap.contains(clientHandler));
  }

  @Test
  public void removeClientHandler_checkIsEmpty() throws IOException {
    Server server = Server.getInstance();
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1, server);

    statusMap.put(clientHandler, 0);
    assert(statusMap.getStatus(clientHandler) == 0);

    statusMap.remove(clientHandler);
    assertFalse(statusMap.contains(clientHandler));
    assert(statusMap.isEmpty());
  }

  @Test
  void putRejectsNullClient() {
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.put(null, 0)
    );
  }

  @Test
  void removeRejectsNullClient() {
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.remove(null)
    );
  }

  @Test
  void removeRejectsClientNotInMap() throws IOException {
    Server server = Server.getInstance();
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler = new ClientHandler(socket, 1, server);

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.remove(clientHandler)
    );
  }

  @Test
  void getStatusRejectsNullClient() {
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.getStatus(null)
    );
  }

  @Test
  void getStatusRejectsClientNotInMap() throws IOException {
    Server server = Server.getInstance();
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler clientHandler = new ClientHandler(socket, 1, server);

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.getStatus(clientHandler)
    );
  }

  @Test
  void containsRejectsNullClient() {
    ClientStatusMap statusMap = ClientStatusMap.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> statusMap.contains(null)
    );
  }
}
