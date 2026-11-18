package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link RoomList} class.
 */
public class RoomListTest {

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
    RoomList instance1 = RoomList.getInstance();
    RoomList instance2 = RoomList.getInstance();

    assertEquals(instance1, instance2);
  }

  @Test
  public void testSingletonDoesNotReturnSameInstanceAfterHavingClosed() throws IOException {
    RoomList instance1 = RoomList.getInstance();
    instance1.resetInstance();
    RoomList instance2 = RoomList.getInstance();

    assertFalse(instance1 == instance2);
  }

  @Test
  public void testAddRoom_checkIdSet() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);

    assertEquals(1L, room.getId());
    assertEquals(1, roomList.size());
  }

  @Test
  public void testRemoveRoomById_checkSize() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);
    assertEquals(1, roomList.size());

    roomList.remove(room.getId());
    assertEquals(0, roomList.size());
  }

  @Test
  public void testRemoveRoomByObject_checkSize() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);
    assertEquals(1, roomList.size());

    roomList.remove(room);
    assertEquals(0, roomList.size());
  }

  @Test
  public void getRoomById_checkCorrectRoom() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);
    assertEquals(room, roomList.get(room.getId()));
  }

  @Test
  public void checkThatNextAvailableIdIncrementsAfterAddingRoom() {
    RoomList roomList = RoomList.getInstance();
    Room room1 = new Room();
    Room room2 = new Room();

    roomList.add(room1);
    long firstId = room1.getId();

    roomList.add(room2);
    long secondId = room2.getId();

    assertEquals(firstId + 1, secondId);
  }

  @Test
  public void testThatGetValuesAndGetKeysReturnUnmodifiableCollections() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);

    assertThrows(UnsupportedOperationException.class, () -> {
      roomList.getValues().add(new Room());
    });

    assertThrows(UnsupportedOperationException.class, () -> {
      roomList.getKeys().add(2L);
    });
  }

  @Test
  public void testThatSizeReturnsCorrectNumberOfRooms() {
    RoomList roomList = RoomList.getInstance();
    assertEquals(0, roomList.size());
  }

  @Test
  public void testContainsKeyMethod() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room();

    roomList.add(room);
    assertTrue(roomList.containsKey(room.getId()));
    assertFalse(roomList.containsKey(999L));
  }

  @Test
  void addRejectsNullRoom() {
    RoomList roomList = RoomList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.add(null)
    );
  }

  @Test
  void addRejectsDuplicateRoomId() {
    RoomList roomList = RoomList.getInstance();
    Room room1 = new Room(1);
    Room room2 = new Room(1);

    roomList.add(room1);

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.add(room2)
    );
  }

  @Test
  void removeObjectRejectsNullRoom() {
    RoomList roomList = RoomList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove((Room) null)
    );
  }

  @Test
  void removeObjectRejectsUnknownRoom() {
    RoomList roomList = RoomList.getInstance();
    Room room = new Room(42);

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove(room)
    );
  }

  @Test
  void removeIdRejectsUnknownId() {
    RoomList roomList = RoomList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove(42L)
    );
  }

  @Test
  void getRejectsUnknownId() {
    RoomList roomList = RoomList.getInstance();

    assertThrows(
        IllegalArgumentException.class,
        () -> roomList.get(42L)
    );
  }
}
