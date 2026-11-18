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
      case "RO-GET_CLIENT_IDS" -> sendClientIds(sender);
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

  /**
   * Removes the {@link ClientHandler} with the ID of the source part of the message from the room.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if the {@link ClientHandler} with the ID of the source part
   *        of the message is not in the room.
   */
  private void removeClient(MessageWorker messageWorker) throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    long clientId = messageWorker.getSourceId();

    if (!this.room.contains(clientId)) {
      throw new IllegalArgumentException(
          "ClientHandler with ID " + clientId + " does not exist in the room."
      );
    }

    this.room.remove(clientId);
  }

  /**
   * Adds the {@link ClientHandler} with the ID of the source part of the message to the room.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   */
  private void addClient(MessageWorker messageWorker) throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    long clientId = messageWorker.getSourceId();
    ClientHandler client = ClientHandlerList.getInstance().get(clientId);
    this.room.add(client);
  }

  /**
   * Sends the IDs of all {@link ClientHandler} objects in the {@link Room}
   * to the given {@link ClientHandler}. The IDs are sent as a dash-separated {@link String}.
   *
   * <p>If there are no {@link ClientHandler} objects in the room, the string "00" is sent instead.
   * </p>
   *
   * @param recipient is the {@link ClientHandler} to send the IDs to.
   * @throws IllegalArgumentException if {@code recipient} is {@code null}.
   */
  private void sendClientIds(ClientHandler recipient) throws IllegalArgumentException {
    if (recipient == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    StringBuilder ids = new StringBuilder();

    if (!this.room.isEmpty()) {
      for (Long id : this.room.getKeys()) {
        ids.append(id).append("-");
      }
    } else {
      ids.append("00");
    }

    MessageWorker messageWorker = new MessageWorker()
        .setDestination("CH", recipient.getId())
        .setSource("RO", this.room.getId())
        .setCommand(ids.toString())
        .buildMessage();

    recipient.sendToClient(messageWorker.getMessage());
  }
}
