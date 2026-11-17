package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;

/**
 * Contains the logic for the {@link Server} class.
 */
public class ServerLogic {

  private Server server;
  private ClientHandlerList handlers;
  private RoomList rooms;
  private ClientStatusMap clientStatusMap;
  private LinkedClients linkedClients;

  /**
   * Instantiates this class.
   * The argument {@code server} is the {@link Server} that this class contains logic for.
   * This class also contains a {@link ClientHandlerList} and a {@link RoomList} for
   * organizing the {@link ClientHandler} and {@link Room} objects associated with the server.
   *
   * @param server is the {@link Server} that this class contains logic for.
   */
  public ServerLogic(Server server) {
    setServer(server);
    this.handlers = ClientHandlerList.getInstance();
    this.rooms = RoomList.getInstance();
    this.clientStatusMap = ClientStatusMap.getInstance();
    this.linkedClients = LinkedClients.getInstance();
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
      case "CH" -> sendToClientHandler(messageWorker);
      case "SE" -> onCommand(messageWorker, sender);
      case "RO" -> sendToRoom(messageWorker, sender);
      case "AL" -> broadcastToAll(messageWorker);
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
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   */
  private void sendToClientHandler(MessageWorker messageWorker) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    if (!this.handlers.containsKey(messageWorker.getDestinationId())) {
      throw new IllegalArgumentException(
          "No ClientHandler with the given ID exists in the handlers map."
      );
    }
    ClientHandler client = this.handlers.get(messageWorker.getDestinationId());
    client.sendToClient(messageWorker.getMessage());
  }

  /**
   * Executes the action associated with the command message of the {@link MessageWorker}.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  public void onCommand(MessageWorker messageWorker, ClientHandler sender) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    String command = messageWorker.getCommand().substring(0,4);

    switch (command)  {
      case "PING" -> pingResponse(messageWorker, sender);
      case "UPDA" -> updateStatus(messageWorker, sender);
      default -> throw new IllegalArgumentException(
          "Command not recognized by server. Unrecognized command: " + command
      );
    }
  }

  /**
   * Sends the given {@link MessageWorker} to the {@link Room} with the ID found in
   * the destination part of the message of the {@link MessageWorker}.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  private void sendToRoom(MessageWorker messageWorker, ClientHandler sender) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    long roomId = messageWorker.getDestinationId();

    if (!this.rooms.containsKey(roomId)) {
      throw new IllegalArgumentException(
          "No Room with the given ID exists in the rooms map."
      );
    }
    Room room = this.rooms.get(roomId);
    room.onCommand(messageWorker, sender);
  }

  /**
   * Broadcasts a message to all connected clients.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   */
  private void broadcastToAll(MessageWorker messageWorker) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    for (ClientHandler client : this.handlers.getValues()) {
      client.sendToClient(messageWorker.getMessage());
    }
  }

  /**
   * Responds to a ping command from a client by sending {@code PONG} back to the client.
   *
   * @param incoming is the {@link MessageWorker} from the client that sent the ping command.
   * @param client is the {@link ClientHandler} that sent the ping command.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   * @throws IllegalArgumentException if {@code incoming} is {@code null}.
   */
  private void pingResponse(MessageWorker incoming, ClientHandler client) {
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }
    if (incoming == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }

    MessageWorker outgoing = new MessageWorker()
        .setSource("SE00")
        .setDestination(incoming.getSource())
        .setCommand("PONG")
        .buildMessage();

    client.sendToClient(outgoing.getMessage());
  }

  /**
   * Updates the status of a client in the {@link ClientStatusMap} based on the command message
   * of the {@link MessageWorker}.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param client is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if {@code messageWorker} is {@code null}.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  private void updateStatus(MessageWorker messageWorker, ClientHandler client) {
    if (messageWorker == null) {
      throw new IllegalArgumentException("MessageWorker cannot be null.");
    }
    if (client == null) {
      throw new IllegalArgumentException("ClientHandler cannot be null.");
    }

    String cmdValue = messageWorker.getCommand().substring(4);
    int status = Integer.parseInt(cmdValue);

    this.clientStatusMap.put(client, status);
    this.linkedClients.checkLinkTriggersAndSendCommandIfTriggered(client);
  }
}
