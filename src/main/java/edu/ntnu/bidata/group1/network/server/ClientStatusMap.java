package edu.ntnu.bidata.group1.network.server;

import java.util.HashMap;

/**
 * A map of the status of all clients connected to the server.
 *
 * <p>
 *   This class is intended to be used as a singleton,
 *   and can be accessed by calling {@link #getInstance()}.
 * </p>
 */
public class ClientStatusMap {

  private static volatile ClientStatusMap instance;

  private HashMap<ClientHandler, Integer> statusMap;

  /**
   * Instantiates the class with an empty map.
   */
  private ClientStatusMap() {
    this.statusMap = new HashMap<>();
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static ClientStatusMap getInstance() {
    if (instance == null) {
      synchronized (ClientStatusMap.class) {
        if (instance == null) {
          instance = new ClientStatusMap();
        }
      }
    }
    return instance;
  }

  /**
   * Either:
   *
   * <ul>
   *   Puts a {@link ClientHandler} into the map and sets its status.
   * </ul>
   *
   * <p>
   *   or:
   * </p>
   *
   * <ul>
   *   Updates the status of an existing {@link ClientHandler} in the map.
   * </ul>
   *
   * @param client is the {@link ClientHandler} in question.
   * @param status is the status to set.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  public void put(ClientHandler client, int status) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    this.statusMap.put(client, status);
  }

  /**
   * Removes a {@link ClientHandler} from the map.
   *
   * @param client is the {@link ClientHandler} to remove.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   * @throws IllegalArgumentException if {@code client} is not in the map.
   */
  public void remove(ClientHandler client) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (!this.statusMap.containsKey(client)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + client.getId() + " does not exist in the map.");
    }
    this.statusMap.remove(client);
  }

  /**
   * Returns the status of a {@link ClientHandler} in the map.
   *
   * @param client is the {@link ClientHandler} to get the status of.
   * @return the status of the {@link ClientHandler},
   *        or {@code null} if the status has not been set yet.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   * @throws IllegalArgumentException if {@code client} is not in the map.
   */
  public Integer getStatus(ClientHandler client) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (!this.statusMap.containsKey(client)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + client.getId() + " does not exist in the map.");
    }
    return this.statusMap.get(client);
  }

  /**
   * Returns the size of the map.
   */
  public int size() {
    return this.statusMap.size();
  }

  /**
   * Checks if the map contains a {@link ClientHandler}.
   *
   * @param client is the {@link ClientHandler} to check for.
   * @return {@code true} if the map contains the {@link ClientHandler}, {@code false} otherwise.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  public boolean contains(ClientHandler client) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    return this.statusMap.containsKey(client);
  }

  /**
   * Checks if the map is empty.
   *
   * @return {@code true} if the map is empty, {@code false} otherwise.
   */
  public boolean isEmpty() {
    return this.statusMap.isEmpty();
  }

  /**
   * Resets the instance of this class to null, allowing for a new instance to be created.
   *
   * <p>
   *   All data in the map will be lost.
   * </p>
   */
  public void resetInstance() {
    instance = null;
  }
}
