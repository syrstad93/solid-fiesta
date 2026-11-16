package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.util.HashMap;

/**
 * A list of the status of all clients connected to the server.
 *
 * <p>
 *   This class is intended to be used as a singleton, and can be accessed by calling {@link #getInstance()}.
 * </p>
 */
public class ClientStatusList {

  private static volatile ClientStatusList instance;

  private HashMap<ClientHandler, Integer> statusMap;

  /**
   * Instantiates the class with an empty map.
   */
  private ClientStatusList() {
    this.statusMap = new HashMap<>();
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static ClientStatusList getInstance() {
    if (instance == null) {
      synchronized (ClientStatusList.class) {
        if (instance == null) {
          instance = new ClientStatusList();
        }
      }
    }
    return instance;
  }

  /**
   * Adds a {@link ClientHandler} to the list.
   * The status is immediately requested from the client,
   * and will be updated when the client responds.
   *
   * @param client is the {@link ClientHandler} to add.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   * @throws IllegalArgumentException if {@code client} is already in the list.
   */
  public void add(ClientHandler client) throws IllegalArgumentException {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (this.statusMap.containsKey(client)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + client.getId() + " already exists in the list.");
    }
    this.statusMap.put(client, null);

    MessageWorker messageWorker = new MessageWorker();

  }

}
