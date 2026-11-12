package edu.ntnu.bidata.group1.network.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A register of all {@link Room} objects.
 * This is a singleton class, and can be accessed by calling {@link #getInstance()}.
 * Contains a {@link Map} with a {@link Room} object as value and its ID as key.
 */
public class RoomList {

  private static volatile RoomList instance;

  private Map<Long, Room> rooms;

  /**
   * Instantiates the class with an empty map.
   */
  private RoomList() {
    this.rooms = new ConcurrentHashMap<>();
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static RoomList getInstance() {
    if (instance == null) {
      synchronized (RoomList.class) {
        if (instance == null) {
          instance = new RoomList();
        }
      }
    }
    return instance;
  }

  /**
   * Puts a {@link Room} in the map with its ID as key.
   *
   * @param room is the {@link Room} to put in the map.
   * @throws IllegalArgumentException if {@code room} is {@code null}.
   * @throws IllegalArgumentException if {@code room} has an ID that is already in the map.
   */
  public void add(Room room) throws IllegalArgumentException {
    if (room == null) {
      throw new IllegalArgumentException("Room cannot be null.");
    }
    if (this.rooms.containsKey(room.getId())) {
      throw new IllegalArgumentException(
          "Room with ID " + room.getId() + " already exists.");
    }
    this.rooms.put(room.getId(), room);
  }

  /**
   * Removes a {@link Room} from the map.
   *
   * @param room is the {@link Room} to remove from the map.
   * @throws IllegalArgumentException if {@code room} is {@code null}.
   * @throws IllegalArgumentException if {@code room} has an ID that is not in the map.
   */
  public void remove(Room room) throws IllegalArgumentException {
    if (room == null) {
      throw new IllegalArgumentException("Room cannot be null.");
    }
    if (!this.rooms.containsKey(room.getId())) {
      throw new IllegalArgumentException(
          "Room with ID " + room.getId() + " does not exist.");
    }
    this.rooms.remove(room.getId());
  }

  /**
   * Removes a {@link Room} from the map by its ID.
   *
   * @param id is the ID of the {@link Room} to remove from the map.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public void remove(long id) throws IllegalArgumentException {
    if (!this.rooms.containsKey(id)) {
      throw new IllegalArgumentException(
          "Room with ID " + id + " does not exist.");
    }
    this.rooms.remove(id);
  }

  /**
   * Returns the {@link Room} with the given ID.
   *
   * @param id is the ID of the {@link Room} to return.
   * @return the {@link Room} with the given ID.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public Room get(long id) throws IllegalArgumentException {
    if (!this.rooms.containsKey(id)) {
      throw new IllegalArgumentException(
          "Room with ID " + id + " does not exist.");
    }
    return this.rooms.get(id);
  }

  /**
   * Returns all the {@link Room} objects in the map.
   *
   * @return all the {@link Room} objects in the map.
   */
  public Collection<Room> getValues() {
    return this.rooms.values();
  }

  /**
   * Returns all the IDs of the {@link Room} objects in the map.
   *
   * @return all the IDs of the {@link Room} objects in the map.
   */
  public Collection<Long> getKeys() {
    return this.rooms.keySet();
  }

}
