package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
}
