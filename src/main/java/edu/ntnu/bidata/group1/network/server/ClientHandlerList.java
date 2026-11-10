package edu.ntnu.bidata.group1.network.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A register of all {@link ClientHandler} objects.
 * Contains a {@link Map} with the ID of the {@link ClientHandler} as key
 * and the {@link ClientHandler} object as value.
 */
public class ClientHandlerList {

  private static volatile ClientHandlerList instance;

  private Map<Long, ClientHandler> handlers;

  /**
   * Instantiates the class with an empty map.
   */
  private ClientHandlerList()  {
    this.handlers = new ConcurrentHashMap<>();
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static ClientHandlerList getInstance() {
    if (instance == null) {
      synchronized (ClientHandlerList.class) {
        if (instance == null) {
          instance = new ClientHandlerList();
        }
      }
    }
    return instance;
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

  /**
   * Checks if the map contains a {@link ClientHandler} with the given ID.
   *
   * @param id is the ID to check for.
   * @return {@code true} if the map contains a {@link ClientHandler} with the given ID,
   *        {@code false} otherwise.
   */
  public boolean containsKey(long id) {
    return this.handlers.containsKey(id);
  }

  /**
   * Returns a {@link Collection} of all {@link ClientHandler} objects in the map.
   *
   * @return a {@link Collection} of all {@link ClientHandler} objects in the map.
   */
  public Collection<ClientHandler> getAll() {
    return this.handlers.values();
  }
}
