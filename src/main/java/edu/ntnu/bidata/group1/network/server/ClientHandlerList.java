package edu.ntnu.bidata.group1.network.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A register of all {@link ClientHandler} objects.
 * Contains a {@link Map} with the ID of the {@link ClientHandler} as key
 * and the {@link ClientHandler} object as value.
 */
public class ClientHandlerList {

  private Map<Long, ClientHandler> handlers;

  /**
   * Instantiates the class with an empty map.
   */
  public ClientHandlerList()  {
    this.handlers = new ConcurrentHashMap<>();
  }

  /**
   * Puts a {@link ClientHandler} in the map with its ID as key.
   *
   * @param handler is the {@link ClientHandler} to put in the map.
   * @throws IllegalArgumentException if {@code handler} is {@code null}.
   * @throws IllegalArgumentException if {@code handler} has an ID that is already in the map.
   */
  public void add(ClientHandler handler) throws IllegalArgumentException {
    if (handler == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (this.handlers.containsKey(handler.getId())) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + handler.getId() + " already exists.");
    }
    this.handlers.put(handler.getId(), handler);
  }

  /**
   * Removes a {@link ClientHandler} from the map.
   *
   * @param handler is the {@link ClientHandler} to remove from the map.
   * @throws IllegalArgumentException if {@code handler} is {@code null}.
   * @throws IllegalArgumentException if {@code handler} is not in the map.
   */
  public void remove(ClientHandler handler) throws IllegalArgumentException {
    if (handler == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (!this.handlers.containsKey(handler.getId())) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + handler.getId() + " does not exist.");
    }
    this.handlers.remove(handler.getId());
  }

  /**
   * Removes a {@link ClientHandler} from the map by its ID.
   *
   * @param id is the ID of the {@link ClientHandler} to remove from the map.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public void remove(long id) throws IllegalArgumentException {
    if (!this.handlers.containsKey(id)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + id + " does not exist.");
    }
    this.handlers.remove(id);
  }

  /**
   * Returns the {@link ClientHandler} with the given ID.
   *
   * @param id is the ID of the {@link ClientHandler} to return.
   * @return the {@link ClientHandler} with the given ID.
   * @throws IllegalArgumentException if {@code id} is not in the map.
   */
  public ClientHandler get(long id) throws IllegalArgumentException {
    if (!this.handlers.containsKey(id)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + id + " does not exist.");
    }
    return this.handlers.get(id);
  }
}
