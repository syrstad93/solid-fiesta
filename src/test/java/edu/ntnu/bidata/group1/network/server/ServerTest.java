package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import edu.ntnu.bidata.group1.utility.Vault;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link Server} class.
 *
 * <p>
 *   AI has assisted in writing the tests,
 *   but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class ServerTest {

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
    Vault.resetInstance();
  }


  @Test
  public void testSingletonInstance() throws IOException {
    Server instance1 = Server.getInstance();
    Server instance2 = Server.getInstance();

    assertEquals(instance1, instance2);
  }

  @Test
  public void testSingletonDoesNotReturnSameInstanceAfterHavingClosed() throws IOException {
    Server instance1 = Server.getInstance();
    instance1.closeServer();
    Server instance2 = Server.getInstance();

    assertEquals(false, instance1 == instance2);
  }

  @Test
  public void testStartMethodRunsWithoutIssues() throws IOException, InterruptedException {
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
    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  public void connectSocketsAndCheckCorrectConnectionsAndCorrectIds() throws IOException, InterruptedException {
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

    // Connect multiple sockets to the server
    int numberOfConnections = 5;
    for (int i = 0; i < numberOfConnections; i++) {
      new java.net.Socket(
          UniversallyAvailableInformation.LOCALHOST, UniversallyAvailableInformation.PORT_NUMBER
      );
    }

    // Allow some time for the server to process connections
    Thread.sleep(1000);

    // Check that the correct number of handlers were created
    assertEquals(numberOfConnections, server.handlers().size());

    // Check that the IDs are assigned correctly
    for (long id = 1; id <= numberOfConnections; id++) {
      assertEquals(id, server.handlers().get(id).getId());
    }
    assertEquals(6, server.peekNextAvailableId());

    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  public void fromSocketSendPingReceivePong() throws IOException, InterruptedException {
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

    // Connect a socket to the server
    java.net.Socket socket = new java.net.Socket(
        UniversallyAvailableInformation.LOCALHOST, UniversallyAvailableInformation.PORT_NUMBER
    );

    // Send a ping message to the server
    java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
    out.println("SE00/0000/PING/=");

    // Read the response from the server
    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
    String response = in.readLine();

    // Check that the response is "pong"
    assertEquals("CH01/SE00/PONG/=", response);

    socket.close();
    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  public void disconnectSocketAndCheckHandlerRemoval() throws IOException, InterruptedException {
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

    // Connect a socket to the server
    java.net.Socket socket = new java.net.Socket(
        UniversallyAvailableInformation.LOCALHOST, UniversallyAvailableInformation.PORT_NUMBER
    );

    // Allow some time for the server to process the connection
    Thread.sleep(1000);

    // Check that the handler was created
    assertEquals(1, server.handlers().size());

    // Disconnect the socket
    socket.close();

    // Allow some time for the server to process the disconnection
    Thread.sleep(1000);

    // Check that the handler was removed
    assertEquals(0, server.handlers().size());

    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  public void closeServerAndCheckSameStateOnReinitializationWithNewServerInstance() throws IOException, InterruptedException {
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

    // Connect a socket to the server
    java.net.Socket socket = new java.net.Socket(
        UniversallyAvailableInformation.LOCALHOST, UniversallyAvailableInformation.PORT_NUMBER
    );

    // Allow some time for the server to process the connection
    Thread.sleep(1000);

    // Check that the handler was created
    assertEquals(1, server.handlers().size());

    // Close the server
    server.closeServer();

    // Reinitialize the server
    Server newServerInstance = Server.getInstance();

    // Check that the new instance has the same state as the previous one
    assertEquals(1, newServerInstance.handlers().size());
    assertEquals(2, newServerInstance.peekNextAvailableId());

    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }

  @Test
  void onMessageRejectsNullMessage() throws IOException {
    Server server = Server.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> server.onMessage(null, null)
    );

    server.closeServer();
  }

  @Test
  void onMessageRejectsNullSender() throws IOException {
    Server server = Server.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> server.onMessage("SE00/0000/PING/=", null)
    );

    server.closeServer();
  }

  @Test
  void onDisconnectRejectsNullClient() throws IOException {
    Server server = Server.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> server.onDisconnect(null)
    );

    server.closeServer();
  }

  @Test
  void startRejectsNullCountDownLatch() throws IOException {
    Server server = Server.getInstance();

    try {
      server.start(null);
    } catch (IllegalArgumentException e) {
      assertEquals("CountDownLatch cannot be null.", e.getMessage());
    } finally {
      server.closeServer();
    }
  }
}
