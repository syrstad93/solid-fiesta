package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.util.Map;

/**
 * Routes a message to its destination based on the destination prefix and ID.
 */
public class RouteMessage {

  private ClientHandlerList handlers;
  private MessageWorker messageWorker;

  /**
   * Instantiates this class by creating a {@link MessageWorker} for the given {@code message},
   * and setting a {@link Map} containing all {@link ClientHandler} objects.
   *
   * @param message is a command message.
   */
  public RouteMessage(String message) {
    setMessageWorker(message);
    this.handlers = ClientHandlerList.getInstance();
  }

  /**
   * Instantiates this class with a {@link MessageWorker}
   * and a {@link Map} containing all {@link ClientHandler} objects.
   *
   * @param messageWorker is a {@link MessageWorker} for a command message.
   */
  public RouteMessage(MessageWorker messageWorker) {
    this.messageWorker = messageWorker;
    this.handlers = ClientHandlerList.getInstance();
  }

  /**
   * Creates a {@link MessageWorker} for the given {@code message}
   * and sets it to {@link #messageWorker}.
   *
   * @param message is a command message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   */
  private void setMessageWorker(String message) throws IllegalArgumentException {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }
    this.messageWorker = new MessageWorker(message);
  }

  /**
   * Routes a message to its destination based on the destination prefix and ID.
   *
   * @param message is a command message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   */
  public void route(String message) throws IllegalArgumentException {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }

    switch (this.messageWorker.getDestinationPrefix())  {
      case "CH" -> sendToClientHandler();
      case "RO" -> sendToRoom();
      case "AL" -> broadcastToAll();
      default -> throw new IllegalArgumentException(
          "Message must contain a valid destination prefix: 'CH', 'SE', 'RO', or 'AL'."
      );
    }
  }

  /**
   * Sends the message of the {@link MessageWorker} to the {@link ClientHandler} with the ID found
   * in the destination part of the message.
   */
  private void sendToClientHandler() {
    if (!this.handlers.containsKey(this.messageWorker.getDestinationId())) {
      throw new IllegalArgumentException(
          "No ClientHandler with the given ID exists in the handlers map."
      );
    }
    ClientHandler client = this.handlers.get(this.messageWorker.getDestinationId());
    client.sendToClient(this.messageWorker.getMessage());
  }
}
