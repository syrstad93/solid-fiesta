package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ClientHandlerList}.
 */
public class ClientHandlerListTest {

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
  public void testSingletonInstance() throws IOException {
    ClientHandlerList instance1 = ClientHandlerList.getInstance();
    ClientHandlerList instance2 = ClientHandlerList.getInstance();
    assertSame(instance1, instance2);
  }

  @Test
  public void testSingletonDoesNotReturnSameInstanceAfterHavingReset() throws IOException {
    ClientHandlerList instance1 = ClientHandlerList.getInstance();
    instance1.resetInstance();
    ClientHandlerList instance2 = ClientHandlerList.getInstance();
    assertSame(false, instance1 == instance2);
  }

  @Test
  public void addClientHandler_checksIfClientHandlerIsAdded() throws IOException {
    ClientHandlerList clientHandlerList = ClientHandlerList.getInstance();

    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1L, server);
    clientHandlerList.add(clientHandler);
    assertSame(clientHandler, clientHandlerList.get(1L));
  }

  @Test
  public void removeClientHandlerWithTheClientHandlerItselfAsArgument() throws IOException {
    ClientHandlerList clientHandlerList = ClientHandlerList.getInstance();

    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1L, server);

    clientHandlerList.add(clientHandler);
    assertEquals(1, clientHandlerList.size());

    clientHandlerList.remove(clientHandler);
    assertEquals(0, clientHandlerList.size());
  }

  @Test
  public void removeClientHandlerWithTheIdAsArgument() throws IOException {
    ClientHandlerList clientHandlerList = ClientHandlerList.getInstance();

    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1L, server);

    clientHandlerList.add(clientHandler);
    assertEquals(1, clientHandlerList.size());

    clientHandlerList.remove(1L);
    assertEquals(0, clientHandlerList.size());
  }

  @Test
  public void checkContainsKeyMethod() throws IOException {
    ClientHandlerList clientHandlerList = ClientHandlerList.getInstance();

    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler clientHandler = new ClientHandler(socket, 1L, server);

    clientHandlerList.add(clientHandler);
    assertTrue(clientHandlerList.containsKey(1L));
  }

  @Test
  void addRejectsNullClientHandler() {
    ClientHandlerList handlers = ClientHandlerList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.add(null)
    );
  }

  @Test
  void addRejectsDuplicateId() throws IOException {
    ClientHandlerList handlers = ClientHandlerList.getInstance();
    Server server = Server.getInstance();

    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client1 = new ClientHandler(socket1, 1L, server);
    handlers.add(client1);

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.add(client1)
    );
  }

  @Test
  void removeHandlerRejectsNullClientHandler() {
    ClientHandlerList handlers = ClientHandlerList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.remove((ClientHandler) null)
    );
  }

  @Test
  void removeHandlerRejectsUnknownClientHandler() throws IOException {
    ClientHandlerList handlers = ClientHandlerList.getInstance();
    Server server = Server.getInstance();

    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1L, server);

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.remove(client)
    );
  }

  @Test
  void removeIdRejectsUnknownId() {
    ClientHandlerList handlers = ClientHandlerList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.remove(42L)
    );
  }

  @Test
  void getRejectsUnknownId() {
    ClientHandlerList handlers = ClientHandlerList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> handlers.get(42L)
    );
  }
}
