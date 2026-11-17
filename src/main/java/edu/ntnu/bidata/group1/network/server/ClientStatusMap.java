package edu.ntnu.bidata.group1.network.server;

import java.util.HashMap;

/**
 * A map of the status of all clients connected to the server.
 *
 * <p>
 *   This class is intended to be used as a singleton, and can be accessed by calling {@link #getInstance()}.
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
   * Puts a {@link ClientHandler} into the map and sets its status.
   *
   * @param client is the {@link ClientHandler} to add.
   * @param status is the status to set.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   * @throws IllegalArgumentException if {@code client} is already in the map.
   */
  public void put(ClientHandler client, int status) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (this.statusMap.containsKey(client)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + client.getId() + " already exists in the map.");
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
}
