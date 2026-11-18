package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link Room} class.
 */
public class RoomTest {

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
  public void testRoomCreation() {
    Room room = new Room(1);
    assert room.getId() == 1;
    assertTrue(room.isEmpty());
  }

  @Test
  public void addTwoClientHandlers_checkCorrect_removeClientsUsingBothRemoveMethods_checkCorrect()
      throws IOException {
    Server server = Server.getInstance();
    Room room = new Room(1);

    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler client1 = new ClientHandler(socket1, 1, server);
    ClientHandler client2 = new ClientHandler(socket2, 2, server);

    room.add(client1);
    room.add(client2);

    assertTrue(room.contains(client1));
    assertTrue(room.contains(client2));

    room.remove(client1);
    room.remove(2);

    assertFalse(room.contains(client1));
    assertFalse(room.contains(client2));
  }

  @Test
  public void testGetValuesAndGetKeys_checkUnmodifiableProperty() throws IOException {
    Server server = Server.getInstance();
    Room room = new Room(1);

    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    Socket socket2 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandler client1 = new ClientHandler(socket1, 1, server);
    ClientHandler client2 = new ClientHandler(socket2, 2, server);

    room.add(client1);
    room.add(client2);

    assertTrue(room.getValues().contains(client1));
    assertTrue(room.getValues().contains(client2));
    assertEquals(2, room.getValues().size());

    assertTrue(room.getKeys().contains(1L));
    assertTrue(room.getKeys().contains(2L));
    assertEquals(2, room.getKeys().size());

    // Check that the returned collections are unmodifiable
    try {
      room.getValues().add(client1);
      fail();
    } catch (UnsupportedOperationException e) {
      // Expected exception, do nothing
    }
    try {
      room.getKeys().add(3L);
      fail();
    } catch (UnsupportedOperationException e) {
      // Expected exception, do nothing
    }
  }

  @Test
  public void clientIsAddedThroughCommandMessage() throws IOException, InterruptedException {
    Server server = Server.getInstance();
    AtomicReference<Exception> serverException = new AtomicReference<>();

    CountDownLatch serverReady = new CountDownLatch(1);

    Thread startThread = new Thread(() -> {
      try {
        server.start(serverReady);
      } catch (IOException e) {
        serverException.set(e);
      }
    });

    startThread.start();
    serverReady.await();

    Room room = new Room(1);
    RoomList roomList = RoomList.getInstance();
    roomList.add(room);

    Socket socket1 = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );

    ClientHandlerList handlerList = ClientHandlerList.getInstance();
    for (int i = 0; i < 1000 && !handlerList.containsKey(1); i++) {
      Thread.sleep(100);
    }

    ClientHandler client1 = ClientHandlerList.getInstance().get(1);

    MessageWorker messageWorker = new MessageWorker()
        .setDestination("RO", room.getId())
        .setSource("CH", client1.getId())
        .setCommand("RO-ADD")
        .buildMessage();

    server.onMessage(messageWorker.getMessage(), client1);

    assertTrue(room.contains(client1));

    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  void addRejectsNullHandler() {
    Room room = new Room(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.add(null)
    );
  }

  @Test
  void addRejectsDuplicateHandler() throws IOException {
    Room room = new Room(1);
    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1, server);

    room.add(client);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.add(client)
    );
  }

  @Test
  void removeRejectsNullHandler() {
    Room room = new Room(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.remove((ClientHandler) null)
    );
  }

  @Test
  void removeRejectsUnknownHandler() throws IOException {
    Room room = new Room(1);
    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1, server);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.remove(client)
    );
  }

  @Test
  void removeRejectsUnknownId() {
    Room room = new Room(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.remove(42)
    );
  }

  @Test
  void getRejectsUnknownId() {
    Room room = new Room(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.get(42)
    );
  }

  @Test
  void containsRejectsNullHandler() {
    Room room = new Room(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.contains((ClientHandler) null)
    );
  }

  @Test
  void onCommandRejectsNullMessage() throws IOException {
    Room room = new Room(1);
    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1, server);

    assertThrows(
        IllegalArgumentException.class,
        () -> room.onCommand(null, client)
    );
  }

  @Test
  void onCommandRejectsNullSender() {
    Room room = new Room(1);
    MessageWorker message = new MessageWorker()
        .setDestination("RO", 1)
        .setSource("CH", 1)
        .setCommand("RO-ADD")
        .buildMessage();

    assertThrows(
        IllegalArgumentException.class,
        () -> room.onCommand(message, null)
    );
  }

  @Test
  void onCommandRejectsUnknownCommand() throws IOException {
    Room room = new Room(1);
    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1, server);

    MessageWorker message = new MessageWorker()
        .setDestination("RO", 1)
        .setSource("CH", 1)
        .setCommand("RO-UNKNOWN")
        .buildMessage();

    assertThrows(
        IllegalArgumentException.class,
        () -> room.onCommand(message, client)
    );
  }

  @Test
  void leaveRejectsClientNotInRoom() throws IOException {
    Room room = new Room(1);
    Server server = Server.getInstance();
    Socket socket = new Socket(
        UniversallyAvailableInformation.LOCALHOST,
        UniversallyAvailableInformation.PORT_NUMBER
    );
    ClientHandler client = new ClientHandler(socket, 1, server);

    MessageWorker message = new MessageWorker()
        .setDestination("RO", 1)
        .setSource("CH", 1)
        .setCommand("RO-LEAVE")
        .buildMessage();

    assertThrows(
        IllegalArgumentException.class,
        () -> room.onCommand(message, client)
    );
  }
}
