package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;

/**
 * Contains the logic for the {@link Server} class.
 */
public class ServerLogic {

  private Server server;

  /**
   * Instantiates this class by setting a {@link Server} to {@link #server}.
   *
   * @param server is the {@link Server} that this class contains logic for.
   */
  public ServerLogic(Server server) {
    setServer(server);
  }

  /**
   * Sets the Server for which this logic class is associated.
   *
   * @param server is the {@link Server} to set.
   * @throws IllegalArgumentException if {@code server} is {@code null}.
   */
  private void setServer(Server server) throws IllegalArgumentException {
    if (server == null) {
      throw new IllegalArgumentException("Server cannot be null.");
    }
    this.server = server;
  }

  /**
   * Returns the {@link Server} for which this logic class is associated.
   *
   * @return the {@link Server} for which this logic class is associated.
   */
  public Server getServer() {
    return this.server;
  }

  /**
   * Receives a command message and executes the associated action.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
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

    // TODO: Logic for handling the command message goes here.
  }
}
