package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;

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
   * Receives a {@link MessageWorker} with a command message for this room and executes the
   * associated action.
   *
   * <ul>
   *   <li>{@link Protocol#JOIN_ROOM} ({@code ROOM-JOIN}): the sender joins the room.
   *       {@code ROOM-ADDED:<roomId>,<senderId>} is broadcast to all clients.</li>
   *   <li>{@link Protocol#LEAVE_ROOM} ({@code ROOM-LEAVE}): the sender leaves the room. A room
   *       that becomes empty is deleted. {@code ROOM-REMOVED:<roomId>,<senderId>} is broadcast to
   *       all clients.</li>
   *   <li>{@link Protocol#GET_ROOM_CLIENTS} ({@code ROOM-CLIENT-LIST}): the sender is sent
   *       {@code ROOM-CLIENTS:<id>,<id>,...}.</li>
   *   <li>{@link Protocol#ROOM_BROADCAST} ({@code ROOM-BROADCAST:<command>}): {@code <command>}
   *       is forwarded to every client in the room, for example
   *       {@code ROOM-BROADCAST:power:false}.</li>
   * </ul>
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} or {@code sender} is {@code null},
   *        or if the command is not a room command.
   */
  public void receiveMessage(MessageWorker messageWorker, ClientHandler sender)
      throws IllegalArgumentException {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    Protocol protocol = messageWorker.getProtocol().orElseThrow(() ->
        new IllegalArgumentException(
            "Command not recognized by room. Unrecognized command: "
                + messageWorker.getCommandKey()));

    switch (protocol) {
      case JOIN_ROOM -> join(sender);
      case LEAVE_ROOM -> leave(sender);
      case GET_ROOM_CLIENTS -> this.room.sendClientIds(sender);
      case ROOM_BROADCAST -> broadcastToClients(messageWorker);
      default -> throw new IllegalArgumentException(
          "Not a room command: " + protocol.getKey());
    }
  }

  /**
   * Adds the sender to the room and tells every client.
   *
   * @param sender is the client joining.
   */
  private void join(ClientHandler sender) {
    this.room.add(sender);
    broadcastToAll(Protocol.ROOM_ADDED.withValue(this.room.getId() + "," + sender.getId()));
  }

  /**
   * Removes the sender from the room, deletes the room if it is now empty, and tells every
   * client.
   *
   * @param sender is the client leaving.
   */
  private void leave(ClientHandler sender) {
    RoomList rooms = RoomList.getInstance();
    synchronized (rooms) {
      this.room.remove(sender.getId());
      if (this.room.isEmpty() && rooms.containsKey(this.room.getId())) {
        rooms.remove(this.room.getId());
      }
    }
    broadcastToAll(Protocol.ROOM_REMOVED.withValue(this.room.getId() + "," + sender.getId()));
  }

  /**
   * Forwards the value of a {@code ROOM-BROADCAST:<command>} message to every client in the room.
   *
   * @param messageWorker is a {@link MessageWorker} with a command message.
   * @throws IllegalArgumentException if there is no command to forward.
   */
  private void broadcastToClients(MessageWorker messageWorker)
      throws IllegalArgumentException {
    String forwarded = messageWorker.getCommandValue();
    if (forwarded.isEmpty()) {
      throw new IllegalArgumentException(
          "Room broadcast needs a command to forward, for example ROOM-BROADCAST:power:false");
    }
    messageWorker.setCommand(forwarded).buildMessage();

    String message = messageWorker.getMessage();
    for (ClientHandler client : this.room.getValues()) {
      client.sendToClient(message);
    }
  }

  /**
   * Sends a message from the server to every connected client.
   *
   * @param command is the command part of the message.
   */
  private void broadcastToAll(String command) {
    String message = new MessageWorker()
        .setSource(MessageWorker.SERVER)
        .setDestination(MessageWorker.BROADCAST)
        .setCommand(command)
        .buildMessage()
        .getMessage();
    for (ClientHandler client : ClientHandlerList.getInstance().getValues()) {
      client.sendToClient(message);
    }
  }
}
