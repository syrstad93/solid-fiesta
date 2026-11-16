package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;

/**
 * Contains the logic for the {@link Room} class.
 */
public class RoomLogic {

  private Room room;

  /**
   * Instantiates this class.
   * The argument {@code room} is the {@link Room} that this class contains logic for.
   *
   * @param room is the {@link Room} that this class contains logic for.
   */
  public RoomLogic(Room room) {
    setRoom(room);
  }

  /**
   * Sets the Room for which this logic class is associated.
   *
   * @param room is the {@link Room} to set.
   * @throws IllegalArgumentException if {@code room} is {@code null}.
   */
  private void setRoom(Room room) throws IllegalArgumentException {
    if (room == null) {
      throw new IllegalArgumentException("Room cannot be null.");
    }
    this.room = room;
  }

  /**
   * Receives a {@link MessageWorker} with a command message and executes the associated action.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  public void receiveMessage(MessageWorker messageWorker, ClientHandler sender)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    String cmd = messageWorker.getCommand();
    String cmdPrefix = cmd.substring(0, 2);

    switch (cmdPrefix) {
      case "RO" -> onCommand(messageWorker, sender);
      case "BC" -> broadcastToClients(messageWorker);
      default -> throw new IllegalArgumentException("Invalid command prefix: " + cmdPrefix);
    }
  }

  /**
   * Receives a {@link MessageWorker} with a command message and executes the associated action.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  private void onCommand(MessageWorker messageWorker, ClientHandler sender)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    String cmd = messageWorker.getCommand();
    switch (cmd) {
      case "RO-LEAVE" -> this.room.remove(sender.getId());
      case "RO-ADD" -> this.room.add(sender);
      case "RO-GET_CLIENT_IDS" -> this.room.sendClientIds(sender);
      default -> throw new IllegalArgumentException("Invalid command: " + cmd);
    }
  }

  /**
   * Broadcasts a message to all {@link ClientHandler} objects in the room.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   */
  private void broadcastToClients(MessageWorker messageWorker)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    String cmd = messageWorker.getCommand().substring(2);
    messageWorker.setCommand(cmd).buildMessage();

    String message = messageWorker.getMessage();
    for (ClientHandler client : this.room.getValues()) {
      client.sendToClient(message);
    }
  }
}
