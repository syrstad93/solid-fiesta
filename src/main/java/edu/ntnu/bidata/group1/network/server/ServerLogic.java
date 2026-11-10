package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;

/**
 * Contains the logic for the {@link Server} class.
 */
public class ServerLogic {

  private Server server;
  private ClientHandlerList handlers;

  /**
   * Instantiates this class by setting a {@link Server} to {@link #server}.
   *
   * @param server is the {@link Server} that this class contains logic for.
   */
  public ServerLogic(Server server) {
    setServer(server);
    this.handlers = ClientHandlerList.getInstance();
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
   * @param message is a command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  public void receiveMessage(String message, ClientHandler sender)
      throws IllegalArgumentException {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    receiveMessage(new MessageWorker(message), sender);
  }

  /**
   * Receives a {@link MessageWorker} for a command message and executes the associated action.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  public void receiveMessage(MessageWorker messageWorker, ClientHandler sender)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    switch (messageWorker.getDestinationPrefix())  {
      case "CH" -> sendToClientHandler(messageWorker, sender);
      case "SE" -> onCommand(messageWorker, sender);
      case "RO" -> sendToRoom(messageWorker, sender);
      case "AL" -> broadcastToAll();
      default -> throw new IllegalArgumentException(
          "Message must contain a valid destination prefix: 'CH', 'SE', 'RO', or 'AL'."
      );
    }
  }

  /**
   * Sends the message of the {@link MessageWorker} to the {@link ClientHandler} with the ID found
   * in the destination part of the message.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  private void sendToClientHandler(MessageWorker messageWorker, ClientHandler sender) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    if (!this.handlers.containsKey(messageWorker.getDestinationId())) {
      throw new IllegalArgumentException(
          "No ClientHandler with the given ID exists in the handlers map."
      );
    }
    ClientHandler client = this.handlers.get(messageWorker.getDestinationId());
    client.sendToClient(messageWorker.getMessage());
  }

}
