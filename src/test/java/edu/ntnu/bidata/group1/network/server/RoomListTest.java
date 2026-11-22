package edu.ntnu.bidata.group1.network.server;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link RoomList} class.
 *
 * <p>
 * AI has assisted in writing the tests,
 * but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class RoomListTest {

  /**
   * Resets the singleton instance after each test.
   */
  @AfterEach
  public void tearDown() {
    RoomList.getInstance().resetInstance();
  }

  /**
   * Tests that getInstance returns the same singleton instance.
   */
  @Test
  public void getInstanceReturnsSameInstance() {
    RoomList instance1 = RoomList.getInstance();
    RoomList instance2 = RoomList.getInstance();

    assertEquals(instance1, instance2);
  }

  /**
   * Tests that resetInstance allows a new instance to be created.
   */
  @Test
  public void resetInstanceCreatesNewInstance() {
    RoomList instance1 = RoomList.getInstance();

    instance1.resetInstance();

    RoomList instance2 = RoomList.getInstance();

    assertNotSame(instance1, instance2);
  }

  /**
   * Tests that a room can be added.
   */
  @Test
  public void addAddsRoomToList() {
    RoomList roomList = RoomList.getInstance();

    Room room = new Room(1);

    roomList.add(room);

    assertTrue(roomList.containsKey(1));
    assertEquals(room, roomList.get(1));
  }

  /**
   * Tests that null rooms are rejected.
   */
  @Test
  public void addRejectsNullRoom() {
    RoomList roomList = RoomList.getInstance();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.add(null)
    );

    assertEquals("Room cannot be null.", exception.getMessage());
  }

  /**
   * Tests that duplicate room ids are rejected.
   */
  @Test
  public void addRejectsDuplicateRoomIds() {
    RoomList roomList = RoomList.getInstance();

    roomList.add(new Room(1));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.add(new Room(1))
    );

    assertEquals("Room with ID 1 already exists.", exception.getMessage());
  }

  /**
   * Tests that create creates a named room.
   */
  @Test
  public void createCreatesNamedRoom() {
    RoomList roomList = RoomList.getInstance();

    Room room = roomList.create("Lobby");

    assertEquals("Lobby", room.getName());
    assertTrue(roomList.containsKey(room.getId()));
  }

  /**
   * Tests that create generates unique ids.
   */
  @Test
  public void createGeneratesUniqueIds() {
    RoomList roomList = RoomList.getInstance();

    Room room1 = roomList.create("Room1");
    Room room2 = roomList.create("Room2");

    assertNotEquals(room1.getId(), room2.getId());
  }

  /**
   * Tests that a room can be removed.
   */
  @Test
  public void removeRemovesRoom() {
    RoomList roomList = RoomList.getInstance();

    Room room = new Room(1);

    roomList.add(room);
    roomList.remove(room);

    assertFalse(roomList.containsKey(1));
  }

  /**
   * Tests that null rooms cannot be removed.
   */
  @Test
  public void removeRejectsNullRoom() {
    RoomList roomList = RoomList.getInstance();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove((Room) null)
    );

    assertEquals("Room cannot be null.", exception.getMessage());
  }

  /**
   * Tests that unknown rooms cannot be removed.
   */
  @Test
  public void removeRejectsUnknownRoom() {
    RoomList roomList = RoomList.getInstance();

    Room room = new Room(99);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove(room)
    );

    assertEquals("Room with ID 99 does not exist.", exception.getMessage());
  }

  /**
   * Tests that a room can be removed by id.
   */
  @Test
  public void removeByIdRemovesRoom() {
    RoomList roomList = RoomList.getInstance();

    Room room = new Room(1);

    roomList.add(room);
    roomList.remove(1);

    assertFalse(roomList.containsKey(1));
  }

  /**
   * Tests that unknown room ids are rejected.
   */
  @Test
  public void removeByIdRejectsUnknownRoom() {
    RoomList roomList = RoomList.getInstance();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.remove(99)
    );

    assertEquals("Room with ID 99 does not exist.", exception.getMessage());
  }

  /**
   * Tests that get returns the correct room.
   */
  @Test
  public void getReturnsCorrectRoom() {
    RoomList roomList = RoomList.getInstance();

    Room room = new Room(1);

    roomList.add(room);

    assertEquals(room, roomList.get(1));
  }

  /**
   * Tests that get rejects unknown room ids.
   */
  @Test
  public void getRejectsUnknownRoomId() {
    RoomList roomList = RoomList.getInstance();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> roomList.get(99)
    );

    assertEquals("Room with ID 99 does not exist.", exception.getMessage());
  }

  /**
   * Tests that getValues returns all rooms.
   */
  @Test
  public void getValuesReturnsAllRooms() {
    RoomList roomList = RoomList.getInstance();

    roomList.add(new Room(1));
    roomList.add(new Room(2));

    assertEquals(2, roomList.getValues().size());
  }

  /**
   * Tests that getKeys returns all room ids.
   */
  @Test
  public void getKeysReturnsAllRoomIds() {
    RoomList roomList = RoomList.getInstance();

    roomList.add(new Room(1));
    roomList.add(new Room(2));

    assertTrue(roomList.getKeys().contains(1L));
    assertTrue(roomList.getKeys().contains(2L));
  }

  /**
   * Tests that containsKey returns true for existing rooms.
   */
  @Test
  public void containsKeyReturnsTrueForExistingRoom() {
    RoomList roomList = RoomList.getInstance();

    roomList.add(new Room(1));

    assertTrue(roomList.containsKey(1));
  }

  /**
   * Tests that containsKey returns false for missing rooms.
   */
  @Test
  public void containsKeyReturnsFalseForMissingRoom() {
    RoomList roomList = RoomList.getInstance();

    assertFalse(roomList.containsKey(999));
  }
}