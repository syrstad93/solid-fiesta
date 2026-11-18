package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A collection of {@link ClientHandler} objects.
 * A Room has a unique ID
 * and a {@link Map} containing all {@link ClientHandler} objects in the room.
 * The map is a {@link ConcurrentHashMap} to ensure thread safety.
 */
public class Room {

  private long id;
  private Map<Long, ClientHandler> handlers;
  private RoomLogic logic;

  /**
   * Instantiates this class by setting an ID and an empty map of {@link ClientHandler} objects.
   *
   * @param id is the ID of this {@link Room}.
   */
  public Room(long id) {
    setId(id);
    this.handlers = new ConcurrentHashMap<>();
    this.logic = new RoomLogic(this);
  }

  /**
   * Sets the ID of this {@link Room}.
   *
   * @param id is the ID to set.
   */
  private void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the ID of this {@link Room}.
   *
   * @return the ID of this {@link Room}.
   */
  public long getId() {
    return this.id;
  }

  /**
   * Adds a {@link ClientHandler} to the map of handlers in this {@link Room}.
   *
   * @param handler is the {@link ClientHandler} to add.
   * @throws IllegalArgumentException if {@code handler} is {@code null}.
   * @throws IllegalArgumentException if {@code handler} has an ID that is already in the map.
   */
  public void add(ClientHandler handler) throws IllegalArgumentException {
    if (handler == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (this.handlers.containsKey(handler.getId())) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + handler.getId() + " already exists in the room.");
    }
    this.handlers.put(handler.getId(), handler);
  }

  /**
   * Removes a {@link ClientHandler} from the map of handlers in this {@link Room}.
   *
   * @param handler is the {@link ClientHandler} to remove.
   * @throws IllegalArgumentException if {@code handler} is {@code null}.
   * @throws IllegalArgumentException if {@code handler} is not in the map.
   */
  public void remove(ClientHandler handler) throws IllegalArgumentException {
    if (handler == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (!this.handlers.containsKey(handler.getId())) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + handler.getId() + " does not exist in the room.");
    }
    this.handlers.remove(handler.getId());
  }

  /**
   * Removes a {@link ClientHandler} from the map of handlers in this {@link Room} by its ID.
   *
   * @param id is the ID of the {@link ClientHandler} to remove.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public void remove(long id) throws IllegalArgumentException {
    if (!this.handlers.containsKey(id)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + id + " does not exist in the room.");
    }
    this.handlers.remove(id);
  }

  /**
   * Returns the {@link ClientHandler} with the given ID
   * from the map of handlers in this {@link Room}.
   *
   * @param id is the ID of the {@link ClientHandler} to return.
   * @return the {@link ClientHandler} with the given ID.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public ClientHandler get(long id) throws IllegalArgumentException {
    if (!this.handlers.containsKey(id)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + id + " does not exist in the room.");
    }
    return this.handlers.get(id);
  }

  /**
   * Returns all {@link ClientHandler} objects in this {@link Room}.
   *
   * @return a {@link Collection} of all {@link ClientHandler} objects.
   */
  public Collection<ClientHandler> getValues() {
    return Collections.unmodifiableCollection(this.handlers.values());
  }

  /**
   * Returns all IDs of the {@link ClientHandler} objects in this {@link Room}.
   *
   * @return a {@link Collection} of all IDs of the {@link ClientHandler} objects.
   */
  public Collection<Long> getKeys() {
    return Collections.unmodifiableCollection(this.handlers.keySet());
  }

  /**
   * Returns the number of {@link ClientHandler} objects in this {@link Room}.
   *
   * @return the number of {@link ClientHandler} objects in this {@link Room}.
   */
  public int size() {
    return this.handlers.size();
  }

  /**
   * Checks if the {@link Room} contains a {@link ClientHandler} with the given ID.
   *
   * @param id is the ID to check for.
   * @return {@code true} if the {@link Room} contains a {@link ClientHandler} with the given ID,
   *         {@code false} otherwise.
   */
  public boolean contains(long id) {
    return this.handlers.containsKey(id);
  }

  /**
   * Checks if the {@link Room} contains a {@link ClientHandler}.
   *
   * @param handler is the {@link ClientHandler} to check for.
   * @return {@code true} if the {@link Room} contains the {@link ClientHandler},
   *         {@code false} otherwise.
   * @throws IllegalArgumentException if {@code handler} is {@code null}.
   */
  public boolean contains(ClientHandler handler) throws IllegalArgumentException {
    if (handler == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    return this.handlers.containsKey(handler.getId());
  }

  /**
   * Checks if the {@link Room} is empty,
   * i.e. if there are any {@link ClientHandler} objects in the map.
   */
  public boolean isEmpty() {
    return this.handlers.isEmpty();
  }

  /**
   * Receives a {@link MessageWorker} and executes the action specified in the message.
   *
   * @param messageWorker is a {@link MessageWorker} containing the message to execute.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  public void onCommand(MessageWorker messageWorker, ClientHandler sender)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    this.logic.receiveMessage(messageWorker, sender);
  }

}
