package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.Automation;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains the logic for the {@link Server} class.
 */
public class ServerLogic {

  private Server server;
  private ClientHandlerList handlers;
  private RoomList rooms;
  private final Set<Long> registeredDevices = new LinkedHashSet<>();
  private final AutomationEngine automations = new AutomationEngine();
  private final Subscriptions subscriptions = new Subscriptions();
  private final ClientDirectory clients = new ClientDirectory();

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

    switch (messageWorker.getDestinationPrefix()) {
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

    Long destinationId = messageWorker.getDestinationId();
    if (destinationId == null) {
      throw new IllegalArgumentException(
          "Destination must contain a client ID, for example CH001.");
    }
    if (!this.handlers.containsKey(destinationId)) {
      throw new IllegalArgumentException(
          "No ClientHandler with the given ID exists in the handlers map."
      );
    }
    ClientHandler client = this.handlers.get(destinationId);
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

    Protocol protocol = messageWorker.getProtocol().orElseThrow(() ->
        new IllegalArgumentException(
            "Command not recognized by server. Unrecognized command: "
                + messageWorker.getCommandKey()));

    // Replies are never answered, not even with an error, so a client's ERROR sent to the server
    // (for example after a rejected automation command) cannot start an error loop.
    if (protocol.isReply()) {
      if (protocol == Protocol.ERROR) {
        System.out.println("Client " + sender.getId() + " reported an error: "
            + messageWorker.getCommandValue());
      }
      return;
    }

    switch (protocol) {
      case HELLO -> hello(messageWorker, sender);
      case PING -> pingResponse(messageWorker, sender);
      case ROOM_CREATE, ROOM_RENAME, ROOM_ADD, ROOM_REMOVE, ROOM_LIST ->
          roomCommand(protocol, messageWorker, sender);
      case DEVICE_ADD, DEVICE_REMOVE, DEVICE_LIST -> deviceCommand(protocol, messageWorker, sender);
      case STATE -> stateUpdate(messageWorker, sender);
      case SUBSCRIBE, UNSUBSCRIBE -> subscriptionCommand(protocol, messageWorker, sender);
      case AUTOMATION_ADD, AUTOMATION_REMOVE, AUTOMATION_LIST ->
          automationCommand(protocol, messageWorker, sender);
      default -> throw new IllegalArgumentException(
          "Command not handled by server: " + protocol.getKey()
      );
    }
  }

  /**
   * Handles room management commands sent to the server.
   *
   * <ul>
   *   <li>{@code ROOM-CREATE:<name>} creates an empty, named room. The server chooses the room's
   *       id. On success, {@code ROOM-CREATED:<roomId>,<name>} is broadcast to all clients.</li>
   *   <li>{@code ROOM-RENAME:<roomId>,<name>} sets or changes the name of an existing room. On
   *       success, {@code ROOM-RENAMED:<roomId>,<name>} is broadcast to all clients.</li>
   *   <li>{@code ROOM-ADD:<roomId>,<clientId>} puts a client in a room. The room is created,
   *       without a name, if it does not exist. On success,
   *       {@code ROOM-ADDED:<roomId>,<clientId>} is broadcast to all clients.</li>
   *   <li>{@code ROOM-REMOVE:<roomId>,<clientId>} takes a client out of a room. A room that
   *       becomes empty is deleted unless it has a name. On success,
   *       {@code ROOM-REMOVED:<roomId>,<clientId>} is broadcast to all clients.</li>
   *   <li>{@code ROOM-LIST} sends the sender one {@code ROOM-CREATED:<roomId>,<name>} message
   *       for every named room, then one {@code ROOM-ADDED:<roomId>,<clientId>} message for every
   *       client in every room.</li>
   * </ul>
   *
   * <p>Which clients are allowed to manage rooms is decided by the controllers' roles; the
   * server does not check it.
   *
   * @param protocol is the room command.
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if the command is malformed, the client does not exist, or
   *        the client is already in (or not in) the room.
   */
  private void roomCommand(Protocol protocol, MessageWorker messageWorker,
      ClientHandler sender) {
    switch (protocol) {
      case ROOM_LIST -> {
        listRooms(sender);
        return;
      }
      case ROOM_CREATE -> {
        createRoom(messageWorker.getCommandValue());
        return;
      }
      case ROOM_RENAME -> {
        renameRoom(messageWorker.getCommandValue());
        return;
      }
      default -> {
      }
    }

    String[] ids = messageWorker.getCommandValue().split(",");
    if (ids.length != 2) {
      throw new IllegalArgumentException(
          "Room command needs a room and a client id, for example ROOM-ADD:1,4");
    }
    long roomId;
    long clientId;
    try {
      roomId = Long.parseLong(ids[0].trim());
      clientId = Long.parseLong(ids[1].trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Room and client id must be numbers.");
    }

    switch (protocol) {
      case ROOM_ADD -> {
        synchronized (this.registeredDevices) {
          if (!this.registeredDevices.contains(clientId)) {
            throw new IllegalArgumentException(
                "Device " + clientId + " must be registered before it can be put in a room.");
          }
        }
        ClientHandler client = this.handlers.get(clientId);
        synchronized (this.rooms) {
          if (!this.rooms.containsKey(roomId)) {
            this.rooms.add(new Room(roomId));
          }
          this.rooms.get(roomId).add(client);
        }
        broadcastFromServer(Protocol.ROOM_ADDED.withValue(roomId + "," + clientId));
      }
      case ROOM_REMOVE -> {
        synchronized (this.rooms) {
          Room room = this.rooms.get(roomId);
          room.remove(clientId);
          if (room.shouldBeDeleted()) {
            this.rooms.remove(roomId);
          }
        }
        broadcastFromServer(Protocol.ROOM_REMOVED.withValue(roomId + "," + clientId));
      }
      default -> throw new IllegalArgumentException("Not a room command: " + protocol.getKey());
    }
  }

  /**
   * Sends the sender every room: {@code ROOM-CREATED:<roomId>,<name>} for each named room, then
   * {@code ROOM-ADDED:<roomId>,<clientId>} for each client in each room.
   *
   * @param sender is the client that asked.
   */
  private void listRooms(ClientHandler sender) {
    List<Room> all;
    synchronized (this.rooms) {
      all = new ArrayList<>(this.rooms.getValues());
    }
    for (Room room : all) {
      if (room.hasName()) {
        sendFromServer(sender,
            Protocol.ROOM_CREATED.withValue(room.getId() + "," + room.getName()));
      }
    }
    for (Room room : all) {
      for (Long clientId : room.getKeys()) {
        sendFromServer(sender, Protocol.ROOM_ADDED.withValue(room.getId() + "," + clientId));
      }
    }
  }

  /**
   * Handles {@code ROOM-CREATE:<name>}: creates an empty room with an id chosen by the server,
   * and broadcasts {@code ROOM-CREATED:<roomId>,<name>}.
   *
   * @param value is the requested name.
   * @throws IllegalArgumentException if the name is not valid.
   */
  private void createRoom(String value) {
    Room room;
    synchronized (this.rooms) {
      room = this.rooms.create(value);
    }
    broadcastFromServer(Protocol.ROOM_CREATED.withValue(room.getId() + "," + room.getName()));
  }

  /**
   * Handles {@code ROOM-RENAME:<roomId>,<name>}: sets the name of an existing room, and broadcasts
   * {@code ROOM-RENAMED:<roomId>,<name>}. Everything after the first comma is the name.
   *
   * @param value is the room id and the new name.
   * @throws IllegalArgumentException if the command is malformed, the room does not exist, or
   *        the name is not valid.
   */
  private void renameRoom(String value) {
    String[] parts = value.split(",", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException(
          "Room name command needs a room id and a name, for example ROOM-RENAME:1,Kitchen");
    }
    long roomId;
    try {
      roomId = Long.parseLong(parts[0].trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Room id must be a number.");
    }
    Room room;
    synchronized (this.rooms) {
      room = this.rooms.get(roomId);
      room.setName(parts[1]);
    }
    broadcastFromServer(Protocol.ROOM_RENAMED.withValue(room.getId() + "," + room.getName()));
  }

  /**
   * Handles the list of registered devices: the devices that controllers in the home can see
   * and control.
   *
   * <ul>
   *   <li>{@code DEVICE-ADD:<clientId>} registers a connected client as a device. On success,
   *       {@code DEVICE-ADDED:<clientId>,<name>,ONLINE} is broadcast to all clients.</li>
   *   <li>{@code DEVICE-REMOVE:<clientId>} unregisters a device, takes it out of every room
   *       ({@code ROOM-REMOVED} is broadcast for each), removes the subscriptions to it, and
   *       removes its automations ({@code AUTOMATION-REMOVED} is broadcast for each). Then
   *       {@code DEVICE-REMOVED:<clientId>} is broadcast to all clients.</li>
   *   <li>{@code DEVICE-LIST} sends the sender one
   *       {@code DEVICE-ADDED:<clientId>,<name>,<ONLINE|OFFLINE>} message for every registered
   *       device.</li>
   * </ul>
   *
   * <p>Which clients are allowed to register devices is decided by the controllers' roles; the
   * server does not check it.
   *
   * @param protocol is the device command.
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if the command is malformed, the client does not exist, or
   *        the device is already (or not) registered.
   */
  private void deviceCommand(Protocol protocol, MessageWorker messageWorker,
      ClientHandler sender) {
    if (protocol == Protocol.DEVICE_LIST) {
      List<Long> devices;
      synchronized (this.registeredDevices) {
        devices = new ArrayList<>(this.registeredDevices);
      }
      for (long deviceId : devices) {
        sendFromServer(sender, Protocol.DEVICE_ADDED.withValue(deviceAddedValue(deviceId)));
      }
      return;
    }

    long deviceId;
    try {
      deviceId = Long.parseLong(messageWorker.getCommandValue());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Device command needs a client id, for example DEVICE-ADD:4");
    }

    switch (protocol) {
      case DEVICE_ADD -> {
        if (!this.handlers.containsKey(deviceId)) {
          throw new IllegalArgumentException("No client with ID " + deviceId + " is connected.");
        }
        if (deviceId == sender.getId()) {
          throw new IllegalArgumentException("A client cannot register itself as a device.");
        }
        synchronized (this.registeredDevices) {
          if (!this.registeredDevices.add(deviceId)) {
            throw new IllegalArgumentException("Device " + deviceId + " is already registered.");
          }
        }
        broadcastFromServer(Protocol.DEVICE_ADDED.withValue(deviceAddedValue(deviceId)));
      }
      case DEVICE_REMOVE -> {
        synchronized (this.registeredDevices) {
          if (!this.registeredDevices.remove(deviceId)) {
            throw new IllegalArgumentException("Device " + deviceId + " is not registered.");
          }
        }
        removeFromAllRooms(deviceId);
        removeDeviceAutomationsAndAnnounce(deviceId);
      }
      default -> throw new IllegalArgumentException("Not a device command: " + protocol.getKey());
    }
  }

  /**
   * Cleans up after a client that lost its connection.
   *
   * <p>A client that introduced itself with {@code HELLO} gets its id back when it reconnects
   * with the same name, so most of its setup is kept:
   * <ul>
   *   <li>its registration, rooms, automations, and the subscriptions to it are kept,</li>
   *   <li>the subscriptions it holds and its stored state are removed; it subscribes and sends
   *       its state again when it reconnects, and</li>
   *   <li>if it is a registered device, {@code DEVICE-OFFLINE:<id>} is broadcast.</li>
   * </ul>
   *
   * <p>A client without a name can never get its id back, so everything about it is removed: it
   * is taken out of every room, and if it was a registered device, it is unregistered and its
   * automations are removed, as with {@code DEVICE-REMOVE}.
   *
   * @param client is the client that disconnected.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  public void onClientDisconnected(ClientHandler client) {
    if (client == null) {
      throw new IllegalArgumentException("Client cannot be null.");
    }
    long id = client.getId();

    if (this.clients.isNamed(id)) {
      this.subscriptions.removeSubscriber(id);
      this.subscriptions.clearState(id);
      if (isRegistered(id)) {
        broadcastFromServer(Protocol.DEVICE_OFFLINE.withValue(id));
      }
      return;
    }

    boolean wasRegistered;
    synchronized (this.registeredDevices) {
      wasRegistered = this.registeredDevices.remove(id);
    }
    removeFromAllRooms(id);
    if (wasRegistered) {
      removeDeviceAutomationsAndAnnounce(id);
    }
    this.subscriptions.removeClient(id);
  }

  /**
   * Handles a client's introduction, {@code HELLO:<name>,<type>} or
   * {@code HELLO:<name>,CONTROLLER,<role>}, and answers with {@code WELCOME:<id>}.
   *
   * <p>If the name has been used before, the client gets that name's id back: its connection is
   * moved to the old id, rooms are pointed at the new connection, and if it is a registered
   * device, {@code DEVICE-ONLINE:<id>} is broadcast. If an older connection with the same name is
   * still open, for example one that died without the server noticing yet, it is sent
   * {@code REPLACED} and closed: the newest connection wins.
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if the introduction is malformed.
   */
  private void hello(MessageWorker messageWorker, ClientHandler sender) {
    ClientDirectory.Client client = ClientDirectory.parseHello(messageWorker.getCommandValue());

    long id;
    ClientHandler previous = null;
    // Same lock as Server.onDisconnect, so an old connection's DEVICE-OFFLINE can never be sent
    // after the DEVICE-ONLINE below.
    synchronized (this.handlers) {
      id = this.clients.introduce(client, sender.getId());
      if (id != sender.getId()) {
        this.handlers.removeIfCurrent(sender);
        sender.changeId(id);
        previous = this.handlers.replace(sender);
      }
    }
    if (previous != null && previous != sender) {
      System.out.println("Client " + client.name() + " connected again; closing its old "
          + "connection.");
      MessageWorker replaced = new MessageWorker()
          .setSource(MessageWorker.SERVER)
          .setDestination("CH", id)
          .setCommand(Protocol.REPLACED.getKey())
          .buildMessage();
      previous.sendToClient(replaced.getMessage());
      previous.close();
    }
    synchronized (this.rooms) {
      for (Room room : this.rooms.getValues()) {
        room.replaceHandler(sender);
      }
    }

    sendFromServer(sender, Protocol.WELCOME.withValue(id));
    if (isRegistered(id)) {
      broadcastFromServer(Protocol.DEVICE_ONLINE.withValue(id));
    }
  }

  /**
   * Sends {@code PING} to every client, and closes the connection to every client that has sent
   * nothing for longer than {@code timeoutMs}. Called regularly by the {@link Server}.
   *
   * @param timeoutMs is how long a client may be silent, in milliseconds.
   */
  public void heartbeat(long timeoutMs) {
    long now = System.currentTimeMillis();
    for (ClientHandler client : new ArrayList<>(this.handlers.getValues())) {
      if (now - client.getLastSeen() > timeoutMs) {
        System.out.println("Client " + this.clients.nameOf(client.getId()) + " has been silent "
            + "for more than " + timeoutMs / 1000 + " s; closing its connection.");
        client.close();
      } else {
        sendFromServer(client, Protocol.PING.getKey());
      }
    }
  }

  /**
   * Returns whether a client is a registered device.
   *
   * @param id is the client's id.
   * @return {@code true} if it is registered.
   */
  private boolean isRegistered(long id) {
    synchronized (this.registeredDevices) {
      return this.registeredDevices.contains(id);
    }
  }

  /**
   * Returns the value of a {@code DEVICE-ADDED} message for a device:
   * {@code <id>,<name>,<ONLINE|OFFLINE>}.
   *
   * @param deviceId is the device's id.
   * @return the value.
   */
  private String deviceAddedValue(long deviceId) {
    String status = this.handlers.containsKey(deviceId) ? "ONLINE" : "OFFLINE";
    return deviceId + "," + this.clients.nameOf(deviceId) + "," + status;
  }

  /**
   * Takes a client out of every room it is in, deletes rooms that become empty, and broadcasts
   * {@code ROOM-REMOVED} for each room it left.
   *
   * @param clientId is the client's id.
   */
  private void removeFromAllRooms(long clientId) {
    List<Long> emptiedRoomIds = new ArrayList<>();
    List<Long> leftRoomIds = new ArrayList<>();
    synchronized (this.rooms) {
      for (Room room : new ArrayList<>(this.rooms.getValues())) {
        if (room.getKeys().contains(clientId)) {
          room.remove(clientId);
          leftRoomIds.add(room.getId());
          if (room.shouldBeDeleted()) {
            emptiedRoomIds.add(room.getId());
          }
        }
      }
      for (long roomId : emptiedRoomIds) {
        this.rooms.remove(roomId);
      }
    }
    for (long roomId : leftRoomIds) {
      broadcastFromServer(Protocol.ROOM_REMOVED.withValue(roomId + "," + clientId));
    }
  }

  /**
   * Removes every subscription to an unregistered device and every automation that uses it, and
   * broadcasts {@code AUTOMATION-REMOVED} for each automation and then {@code DEVICE-REMOVED}.
   *
   * @param deviceId is the id of the device that was unregistered.
   */
  private void removeDeviceAutomationsAndAnnounce(long deviceId) {
    this.subscriptions.removeSubscriptionsTo(deviceId);
    for (Automation removed : this.automations.removeInvolving(deviceId)) {
      broadcastFromServer(Protocol.AUTOMATION_REMOVED.withValue(removed.getId()));
    }
    broadcastFromServer(Protocol.DEVICE_REMOVED.withValue(deviceId));
  }

  /**
   * Handles a device's state, {@code STATE:<property>,<value>}.
   *
   * <ol>
   *   <li>The value is stored, so it can be sent to clients that subscribe later.</li>
   *   <li>It is sent to every subscriber as {@code <property>:<value>}, with the device as the
   *       source, for example {@code CH005/CH001/powerOn:true/=}.</li>
   *   <li>If the value is a number, every automation whose condition it makes true fires.</li>
   * </ol>
   *
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the device that sent its state.
   * @throws IllegalArgumentException if the state is malformed.
   */
  private void stateUpdate(MessageWorker messageWorker, ClientHandler sender) {
    String[] parts = messageWorker.getCommandValue().split(",", 2);
if (parts.length != 2 || parts[0].isBlank()
        || parts[0].chars().anyMatch(c -> "/=:\n\r".indexOf(c) >= 0)) {
      throw new IllegalArgumentException(
          "State needs a property and a value, for example STATE:powerOn,true");
    }
    String property = parts[0].trim();
    String value = parts[1].trim();
    long deviceId = sender.getId();

    this.subscriptions.updateState(deviceId, property, value);
    for (long subscriberId : this.subscriptions.subscribersOf(deviceId)) {
      sendState(subscriberId, deviceId, property, value);
    }

    double number;
    try {
      number = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return;
    }
    if (Double.isNaN(number) || Double.isInfinite(number)) {
      return;
    }
    for (Automation automation : this.automations.onReading(deviceId, property, number)) {
      fire(automation, number);
    }
  }

  /**
   * Handles subscriptions to a device's state.
   *
   * <ul>
   *   <li>{@code SUBSCRIBE:<deviceId>} subscribes the sender to a registered device, and sends
   *       it the device's latest state at once, one {@code <property>:<value>} message per
   *       property. Subscribing again just sends the state again.</li>
   *   <li>{@code UNSUBSCRIBE:<deviceId>} stops sending the device's state to the sender. It is
   *       not an error to unsubscribe from a device one is not subscribed to.</li>
   * </ul>
   *
   * @param protocol is the subscription command.
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if the command is malformed, or the device to subscribe to
   *        is not registered.
   */
  private void subscriptionCommand(Protocol protocol, MessageWorker messageWorker,
      ClientHandler sender) {
    long deviceId;
    try {
      deviceId = Long.parseLong(messageWorker.getCommandValue());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Subscription command needs a device id, for example SUBSCRIBE:4");
    }
    switch (protocol) {
      case SUBSCRIBE -> {
        synchronized (this.registeredDevices) {
          if (!this.registeredDevices.contains(deviceId)) {
            throw new IllegalArgumentException(
                "Device " + deviceId + " is not a registered device.");
          }
        }
        this.subscriptions.subscribe(sender.getId(), deviceId);
        for (Map.Entry<String, String> entry : this.subscriptions.stateOf(deviceId).entrySet()) {
          sendState(sender.getId(), deviceId, entry.getKey(), entry.getValue());
        }
      }
      case UNSUBSCRIBE -> this.subscriptions.unsubscribe(sender.getId(), deviceId);
      default -> throw new IllegalArgumentException(
          "Not a subscription command: " + protocol.getKey());
    }
  }

  /**
   * Sends one property of a device's state to a subscriber, with the device as the source.
   * Nothing is sent if the subscriber is no longer connected.
   *
   * @param subscriberId is the client to send to.
   * @param deviceId is the device the state belongs to.
   * @param property is the property's name.
   * @param value is the property's value.
   */
  private void sendState(long subscriberId, long deviceId, String property, String value) {
    if (!this.handlers.containsKey(subscriberId)) {
      return;
    }
    MessageWorker outgoing = new MessageWorker()
        .setSource("CH", deviceId)
        .setDestination("CH", subscriberId)
        .setCommand(property + ":" + value)
        .buildMessage();
    this.handlers.get(subscriberId).sendToClient(outgoing.getMessage());
  }

  /**
   * Handles the automation rules.
   *
   * <ul>
   *   <li>{@code AUTOMATION-ADD:<sensor>,<reading>,<ABOVE|BELOW>,<threshold>,<device>,<command>}
   *       adds a rule. Both the sensor and the device must be registered. On success,
   *       {@code AUTOMATION-ADDED:<id>,...} is broadcast to all clients. If the sensor's latest
   *       reading already meets the condition, the automation fires right away.</li>
   *   <li>{@code AUTOMATION-REMOVE:<id>} removes a rule. On success,
   *       {@code AUTOMATION-REMOVED:<id>} is broadcast to all clients.</li>
   *   <li>{@code AUTOMATION-LIST} sends the sender one {@code AUTOMATION-ADDED} message per
   *       rule.</li>
   * </ul>
   *
   * <p>Which clients may manage automations is decided by the controllers' roles; the server
   * does not check it.
   *
   * @param protocol is the automation command.
   * @param messageWorker is a {@link MessageWorker} for the command message.
   * @param sender is the {@link ClientHandler} that sent the message.
   * @throws IllegalArgumentException if the command is malformed, a device is not registered,
   *        or the automation does not exist.
   */
  private void automationCommand(Protocol protocol, MessageWorker messageWorker,
      ClientHandler sender) {
    switch (protocol) {
      case AUTOMATION_LIST -> {
        for (Automation automation : this.automations.getAll()) {
          sendFromServer(sender, Protocol.AUTOMATION_ADDED.withValue(automation.toFullValue()));
        }
      }
      case AUTOMATION_ADD -> {
        Automation requested = Automation.fromRequestValue(messageWorker.getCommandValue());
        synchronized (this.registeredDevices) {
          if (!this.registeredDevices.contains(requested.getSensorId())) {
            throw new IllegalArgumentException(
                "Sensor " + requested.getSensorId() + " is not a registered device.");
          }
          if (!this.registeredDevices.contains(requested.getDeviceId())) {
            throw new IllegalArgumentException(
                "Device " + requested.getDeviceId() + " is not a registered device.");
          }
        }
        Automation stored = this.automations.add(requested);
        broadcastFromServer(Protocol.AUTOMATION_ADDED.withValue(stored.toFullValue()));
        for (Automation automation : this.automations.evaluateNow(stored.getId())) {
          fire(automation, this.automations.latestReading(automation.getSensorId(),
              automation.getReading()));
        }
      }
      case AUTOMATION_REMOVE -> {
        long id;
        try {
          id = Long.parseLong(messageWorker.getCommandValue());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              "Automation command needs an automation id, for example AUTOMATION-REMOVE:2");
        }
        this.automations.remove(id);
        broadcastFromServer(Protocol.AUTOMATION_REMOVED.withValue(id));
      }
      default -> throw new IllegalArgumentException(
          "Not an automation command: " + protocol.getKey());
    }
  }

  /**
   * Sends an automation's command to its device, from the server, and tells every client that
   * the automation fired.
   *
   * @param automation is the automation that fired.
   * @param value is the reading that made it fire.
   */
  private void fire(Automation automation, double value) {
    long deviceId = automation.getDeviceId();
    if (!this.handlers.containsKey(deviceId)) {
      System.out.println("Automation " + automation.getId() + " fired, but device " + deviceId
          + " is not connected.");
      return;
    }
    ClientHandler device = this.handlers.get(deviceId);
    MessageWorker command = new MessageWorker()
        .setSource(MessageWorker.SERVER)
        .setDestination("CH", deviceId)
        .setCommand(automation.getCommand())
        .buildMessage();
    device.sendToClient(command.getMessage());

    broadcastFromServer(
        Protocol.AUTOMATION_TRIGGERED.withValue(automation.getId() + "," + value));
  }

  /**
   * Sends a message from the server to one client.
   *
   * @param recipient is the client to send to.
   * @param command is the command part of the message.
   */
  private void sendFromServer(ClientHandler recipient, String command) {
    MessageWorker outgoing = new MessageWorker()
        .setSource(MessageWorker.SERVER)
        .setDestination("CH", recipient.getId())
        .setCommand(command)
        .buildMessage();
    recipient.sendToClient(outgoing.getMessage());
  }

  /**
   * Sends a message from the server to every connected client.
   *
   * @param command is the command part of the message.
   */
  private void broadcastFromServer(String command) {
    MessageWorker outgoing = new MessageWorker()
        .setSource(MessageWorker.SERVER)
        .setDestination(MessageWorker.BROADCAST)
        .setCommand(command)
        .buildMessage();
    broadcastToAll(outgoing);
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

    Long roomId = messageWorker.getDestinationId();
    if (roomId == null) {
      throw new IllegalArgumentException(
          "Destination must contain a room ID, for example RO010.");
    }

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
        .setSource(MessageWorker.SERVER)
        .setDestination(incoming.getSource())
        .setCommand(Protocol.PONG.getKey())
        .buildMessage();

    client.sendToClient(outgoing.getMessage());
  }
}
