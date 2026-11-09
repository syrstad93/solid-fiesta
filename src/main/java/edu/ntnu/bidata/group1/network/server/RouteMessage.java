package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.util.Map;

/**
 * Routes a message to its destination based on the destination prefix and ID.
 */
public class RouteMessage {

  private Map<Long, ClientHandler> handlers;
  private MessageWorker messageWorker;

  /**
   * Instantiates this class by instantiating a {@link MessageWorker} for the given {@code message},
   * and setting a {@link Map} containing all {@link ClientHandler} objects.
   *
   * @param message is a command message.
   * @param handlers is a {@link Map} containing all {@link ClientHandler} objects.
   */
  public RouteMessage(String message, Map<Long, ClientHandler> handlers) {
    setMessageWorker(message);
    setHandlers(handlers);
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
   * Sets an unmodifiable {@link Map}
   * containing all {@link ClientHandler} objects to {@link #handlers}.
   *
   * @param handlers is a {@link Map} containing all {@link ClientHandler} objects.
   * @throws IllegalArgumentException if {@code handlers} is {@code null}.
   */
  private void setHandlers(Map<Long, ClientHandler> handlers) throws IllegalArgumentException {
    if (handlers == null) {
      throw new IllegalArgumentException("Handlers cannot be null.");
    }
    this.handlers = Map.copyOf(handlers);
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
      case "SE" -> sendToServer();
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
