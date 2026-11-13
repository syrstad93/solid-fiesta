package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link Server} class.
 */
public class ServerTest {

  @Test
  public void testSingletonInstance() throws IOException {
    Server instance1 = Server.getInstance();
    Server instance2 = Server.getInstance();

    assertEquals(instance1, instance2);
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

    server.closeServer();
    startThread.join();

    if (serverException.get() != null) {
      throw new RuntimeException("Server start method threw an exception", serverException.get());
    }
  }
}
