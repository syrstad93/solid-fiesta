package edu.ntnu.bidata.group1.device.controller;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.device.actuator.CommandSpec;
import edu.ntnu.bidata.group1.device.actuator.ParameterType;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.utility.Automation;
import edu.ntnu.bidata.group1.utility.Condition;
import edu.ntnu.bidata.group1.utility.DeviceType;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Permission;
import edu.ntnu.bidata.group1.utility.Protocol;
import edu.ntnu.bidata.group1.utility.Role;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A device that controls other devices by sending them messages through the server.
 *
 * <p>A controller holds:
 * <ul>
 *   <li>a {@link Role} that is fixed when the controller is created and decides what it may
 *       do,</li>
 *   <li>the list of devices in the home ({@link ControlledDevice}), each with the commands that
 *       device has announced and its last reported state,</li>
 *   <li>the rooms the devices have been put in, and</li>
 *   <li>the automations the server follows.</li>
 * </ul>
 *
 * <p>The devices, rooms and automations are kept by the server and shared by every controller:
 * when an owner adds a device or a user puts one in a room, the server tells every controller,
 * so a guest can control the devices an owner added.
 *
 * <table>
 *   <caption>What each role may do</caption>
 *   <tr><th>Method</th><th>GUEST</th><th>USER</th><th>OWNER</th></tr>
 *   <tr><td>{@link #getDevices()}, {@link #getRooms()}, {@link #getRoomNames()},
 *       {@link #getAutomations()}</td>
 *       <td>yes</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>{@link #sendCommand(long, String, String)}, {@link #subscribe(long)},
 *       {@link #unsubscribe(long)}</td>
 *       <td>yes</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>{@link #createRoom(String)}, {@link #renameRoom(long, String)},
 *       {@link #addDeviceToRoom(long, long)}, {@link #removeDeviceFromRoom(long, long)}</td>
 *       <td>no</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>{@link #addAutomation(long, String, Condition, double, long, String, String)},
 *       {@link #removeAutomation(long)}</td><td>no</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>{@link #addDevice(long)}, {@link #removeDevice(long)}</td>
 *       <td>no</td><td>no</td><td>yes</td></tr>
 * </table>
 * A method that the role does not allow throws {@link AccessDeniedException}. Every method
 * that sends something throws {@link IllegalStateException} before {@link #start()}.
 *
 * <h2>Typical use</h2>
 * <pre>{@code
 * Controller owner = new Controller("anna-phone", Role.OWNER);
 * Controller user = new Controller("ola-laptop", Role.USER);
 * Controller guest = new Controller("visitor-tablet", Role.GUEST);
 * owner.start();
 * user.start();
 * guest.start();
 *
 * owner.addDevice(1);                 // register client 1; every controller now knows it
 * user.addDevice(2);                  // throws AccessDeniedException: only owners add devices
 *
 * long kitchen = user.createRoom("Kitchen").join(); // the server chooses the id
 * user.addDeviceToRoom(kitchen, 1);   // put device 1 in the kitchen
 * user.renameRoom(kitchen, "Kitchen and dining");
 * guest.addDeviceToRoom(10, 1);       // throws AccessDeniedException
 *
 * guest.sendCommand(1, "value", "75"); // guests can control devices
 * }</pre>
 *
 * <p>Device ids are the client ids the server assigns, used in message addresses as
 * {@code CH<id>}. The controller only keeps information from registered devices; messages from
 * other clients are ignored.
 *
 * <h2>State</h2>
 * The controller subscribes to every registered device with {@code SUBSCRIBE:<deviceId>}. The
 * server then sends it each device's state as {@code <property>:<value>} messages, with the
 * device as the source, first the latest values and then every change. They are stored in the
 * device's {@link ControlledDevice}.
 *
 * <h2>Device messages</h2>
 * All message names are listed in {@link Protocol}. An owner registers and unregisters devices
 * with {@code DEVICE-ADD:<deviceId>} and {@code DEVICE-REMOVE:<deviceId>}. The server
 * broadcasts {@code DEVICE-ADDED:<deviceId>} or {@code DEVICE-REMOVED:<deviceId>} to every
 * client. A registered device that loses its connection stays registered and is shown as offline
 * ({@code DEVICE-OFFLINE}, {@link ControlledDevice#isOnline()}). A new controller
 * asks for the current devices with {@code DEVICE-LIST}.
 *
 * <h2>Room messages</h2>
 * Room changes are sent to the server as {@code ROOM-ADD:<roomId>,<deviceId>} and
 * {@code ROOM-REMOVE:<roomId>,<deviceId>}. The server broadcasts
 * {@code ROOM-ADDED:<roomId>,<deviceId>} or {@code ROOM-REMOVED:<roomId>,<deviceId>} to every
 * client, so all controllers, whatever their role, see the same rooms. {@link #getRooms()} only
 * changes when that confirmation arrives. If the server rejects a change, it answers the sender
 * with {@code ERROR:ROOM-ADD,<reason>} instead. A new controller asks for the current rooms with
 * {@code ROOM-LIST}.
 *
 * <h2>Automations</h2>
 * An automation is a rule the server follows on its own, for example "when {@code temperature}
 * on sensor 3 goes above 25, send {@code power:true} to device 1". It is added with
 * {@code AUTOMATION-ADD} and removed with {@code AUTOMATION-REMOVE}. As with rooms, the server
 * broadcasts {@code AUTOMATION-ADDED} and {@code AUTOMATION-REMOVED}, and only then does
 * {@link #getAutomations()} change. A new controller asks for them with
 * {@code AUTOMATION-LIST}. See {@link Automation}.
 */
public class Controller extends Device {

  private final Role role;

  private final Map<Long, ControlledDevice> devices = new LinkedHashMap<>();
  private final Map<Long, Set<Long>> rooms = new LinkedHashMap<>();
  private final Map<Long, String> roomNames = new LinkedHashMap<>();
  private final Map<Long, Automation> automations = new LinkedHashMap<>();
  private final PendingRequests pending = new PendingRequests();

  /**
   * Creates a controller with the given role and a random name, such as
   * {@code controller-3f9a1c2e}. See {@link #Controller(String, Role)}.
   *
   * @param role what this controller is allowed to do; cannot be changed later
   * @throws IllegalArgumentException if {@code role} is null
   */
  public Controller(Role role) {
    this(randomName("controller"), role);
  }

  /**
   * Creates a controller with the given name and role. Call {@link #start()} to connect it; it
   * then introduces itself with {@code HELLO:<name>,CONTROLLER,<role>} and asks the server for
   * the home's devices, rooms and automations.
   *
   * @param name the controller's name, for example {@code anna-phone}
   * @param role what this controller is allowed to do; cannot be changed later
   * @throws IllegalArgumentException if {@code role} is null or the name is not allowed
   */
  public Controller(String name, Role role) {
    super(name, DeviceType.CONTROLLER);
    this.role = requireRole(role);
  }

  /**
   * Adds the role to the {@code HELLO} message: {@code <name>,CONTROLLER,<role>}.
   *
   * @return the value, for example {@code anna-phone,CONTROLLER,OWNER}
   */
  @Override
  protected String helloValue() {
    return super.helloValue() + "," + role.name();
  }

  /**
   * Asks the server for the home's devices, rooms and automations. Runs once, when the
   * controller is started.
   */
  @Override
  protected void onStarted() {
    super.onStarted();
    requestHomeState();
  }

  private static Role requireRole(Role role) {
    if (role == null) {
      throw new IllegalArgumentException("Role cannot be null.");
    }
    return role;
  }

  /**
   * Returns this controller's role.
   *
   * @return the role set when the controller was created
   */
  public Role getRole() {
    return role;
  }

  /**
   * Returns whether this controller's role allows the given permission.
   *
   * @param permission the permission to check
   * @return {@code true} if allowed
   */
  public boolean can(Permission permission) {
    return role.has(permission);
  }

  /**
   * Asks the server to register a connected client as a device in the home. Once the server
   * confirms, every controller adds it to its device list and asks it for its commands and
   * state.
   *
   * @param deviceId the device's client id
   * @return completes with the device's record when the server confirms, or fails with a
   *     {@link RequestFailedException} if the server refuses or does not answer in time
   * @throws AccessDeniedException unless the role is {@link Role#OWNER}
   * @throws IllegalStateException if the controller has not been started
   */
  public CompletableFuture<ControlledDevice> addDevice(long deviceId) {
    require(Permission.MANAGE_DEVICES, "add devices");
    CompletableFuture<String> answer =
        pending.add(Protocol.DEVICE_ADD, Protocol.DEVICE_ADDED, String.valueOf(deviceId));
    sendMessage(MessageWorker.SERVER, Protocol.DEVICE_ADD.withValue(deviceId));
    // By the time the reply arrives, handleServerMessage has added the device to the list.
    return answer.thenApply(reply -> findDevice(deviceId));
  }

  /**
   * Asks the server to unregister a device. The server also takes it out of every room. Once
   * the server confirms, every controller removes it from its device list.
   *
   * @param deviceId the device's client id
   * @return completes when the server confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException unless the role is {@link Role#OWNER}
   * @throws IllegalArgumentException if the device is not registered
   */
  public CompletableFuture<Void> removeDevice(long deviceId) {
    require(Permission.MANAGE_DEVICES, "remove devices");
    requireControlled(deviceId);
    CompletableFuture<String> answer =
        pending.add(Protocol.DEVICE_REMOVE, Protocol.DEVICE_REMOVED, String.valueOf(deviceId));
    sendMessage(MessageWorker.SERVER, Protocol.DEVICE_REMOVE.withValue(deviceId));
    return answer.thenAccept(reply -> { });
  }

  /**
   * Asks the server to create an empty room with the given name. The server chooses the room's
   * id and broadcasts {@code ROOM-CREATED:<roomId>,<name>}; {@link #getRooms()} and
   * {@link #getRoomNames()} change once it does.
   *
   * @param name the room's name; leading and trailing whitespace is removed
   * @return completes with the new room's id when the server confirms, or fails with a
   *     {@link RequestFailedException} if the server refuses or does not answer in time
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   * @throws IllegalArgumentException if the name is blank or contains {@code /}, {@code =} or
   *     line breaks
   */
  public CompletableFuture<Long> createRoom(String name) {
    require(Permission.MANAGE_ROOMS, "create rooms");
    String trimmed = validateRoomName(name);
    CompletableFuture<String> answer =
        pending.add(Protocol.ROOM_CREATE, Protocol.ROOM_CREATED, trimmed);
    sendMessage(MessageWorker.SERVER, Protocol.ROOM_CREATE.withValue(trimmed));
    return answer.thenApply(reply -> Long.parseLong(reply.substring(0, reply.indexOf(','))));
  }

  /**
   * Asks the server to set or change the name of a room. The server broadcasts
   * {@code ROOM-RENAMED:<roomId>,<name>}; {@link #getRoomNames()} changes once it does.
   *
   * @param roomId the room's id
   * @param newName the new name; leading and trailing whitespace is removed
   * @return completes when the server confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   * @throws IllegalArgumentException if the room is unknown, or the name is blank or contains
   *     {@code /}, {@code =} or line breaks
   */
  public CompletableFuture<Void> renameRoom(long roomId, String newName) {
    require(Permission.MANAGE_ROOMS, "rename rooms");
    String trimmed = validateRoomName(newName);
    synchronized (rooms) {
      if (!rooms.containsKey(roomId)) {
        throw new IllegalArgumentException("Room " + roomId + " does not exist.");
      }
    }
    String value = roomId + "," + trimmed;
    CompletableFuture<String> answer =
        pending.add(Protocol.ROOM_RENAME, Protocol.ROOM_RENAMED, value);
    sendMessage(MessageWorker.SERVER, Protocol.ROOM_RENAME.withValue(value));
    return answer.thenAccept(reply -> { });
  }

  /**
   * Asks the server to put a registered device in a room. The room is created if it does not
   * exist yet. {@link #getRooms()} changes once the server confirms.
   *
   * @param roomId the room's id
   * @param deviceId the device's client id; must be a registered device
   * @return completes when the server confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   * @throws IllegalArgumentException if the device is not registered
   */
  public CompletableFuture<Void> addDeviceToRoom(long roomId, long deviceId) {
    require(Permission.MANAGE_ROOMS, "add devices to rooms");
    requireControlled(deviceId);
    String value = roomId + "," + deviceId;
    CompletableFuture<String> answer = pending.add(Protocol.ROOM_ADD, Protocol.ROOM_ADDED, value);
    sendMessage(MessageWorker.SERVER, Protocol.ROOM_ADD.withValue(value));
    return answer.thenAccept(reply -> { });
  }

  /**
   * Asks the server to take a device out of a room. {@link #getRooms()} changes once the server
   * confirms.
   *
   * @param roomId the room's id
   * @param deviceId the device's client id
   * @return completes when the server confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   */
  public CompletableFuture<Void> removeDeviceFromRoom(long roomId, long deviceId) {
    require(Permission.MANAGE_ROOMS, "remove devices from rooms");
    String value = roomId + "," + deviceId;
    CompletableFuture<String> answer =
        pending.add(Protocol.ROOM_REMOVE, Protocol.ROOM_REMOVED, value);
    sendMessage(MessageWorker.SERVER, Protocol.ROOM_REMOVE.withValue(value));
    return answer.thenAccept(reply -> { });
  }

  /**
   * Returns the rooms and the devices in each, as confirmed by the server. Every role may call
   * this.
   *
   * @return an unmodifiable copy: room id to the ids of the devices in it
   */
  public Map<Long, Set<Long>> getRooms() {
    Map<Long, Set<Long>> copy = new LinkedHashMap<>();
    synchronized (rooms) {
      for (Map.Entry<Long, Set<Long>> entry : rooms.entrySet()) {
        copy.put(entry.getKey(),
            Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
      }
    }
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Returns the names of the rooms that have one, as confirmed by the server. Rooms created
   * implicitly by {@link #addDeviceToRoom(long, long)} have no name until
   * {@link #renameRoom(long, String)} gives them one. Every role may call this.
   *
   * @return an unmodifiable copy: room id to room name
   */
  public Map<Long, String> getRoomNames() {
    Map<Long, String> copy;
    synchronized (roomNames) {
      copy = new LinkedHashMap<>(roomNames);
    }
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Returns the devices in one room. Every role may call this.
   *
   * @param roomId the room's id
   * @return the registered devices in the room; empty if the room is unknown
   */
  public List<ControlledDevice> getDevicesInRoom(long roomId) {
    List<ControlledDevice> result = new ArrayList<>();
    for (long deviceId : getRooms().getOrDefault(roomId, Set.of())) {
      ControlledDevice device = findDevice(deviceId);
      if (device != null) {
        result.add(device);
      }
    }
    return result;
  }

  /**
   * Asks the server for every registered device, every room and the devices in it, and every
   * automation. This is done automatically when the controller is created; after that, the
   * server keeps every controller up to date.
   */
  public void requestHomeState() {
    sendMessage(MessageWorker.SERVER, Protocol.DEVICE_LIST.getKey());
    sendMessage(MessageWorker.SERVER, Protocol.ROOM_LIST.getKey());
    sendMessage(MessageWorker.SERVER, Protocol.AUTOMATION_LIST.getKey());
  }

  /**
   * Asks the server to add an automation: when {@code reading} on {@code sensorId} goes above or
   * below {@code threshold}, the server sends {@code command:argument} to {@code deviceId}.
   * {@link #getAutomations()} changes once the server confirms.
   *
   * <p>The watched device is usually a sensor, but can be any registered device that reports
   * {@code reading} as a number, for example a light's {@code value}.
   *
   * <p>The command and argument are checked against the device's announced commands before
   * anything is sent, the same way as in {@link #sendCommand(long, String, String)}.
   *
   * @param sensorId the client id of the device to watch, usually a sensor; must be registered
   * @param reading the reading to watch, for example {@code temperature}
   * @param condition {@link Condition#ABOVE} or {@link Condition#BELOW}
   * @param threshold the threshold
   * @param deviceId the client id of the device to control; must be a registered device
   * @param command the command to send, as announced by the device, for example {@code power}
   * @param argument the command's argument, for example {@code true}; ignored for commands
   *     without a parameter
   * @return completes with the automation, including the id the server gave it, when the server
   *     confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   * @throws IllegalArgumentException if a device is not registered, the watched device is known
   *     not to report this reading as a number, or the command does not match the device's
   *     announced commands
   */
  public CompletableFuture<Automation> addAutomation(long sensorId, String reading,
      Condition condition, double threshold, long deviceId, String command, String argument) {
    require(Permission.MANAGE_AUTOMATIONS, "add automations");

    ControlledDevice sensor = requireControlled(sensorId);
    Map<String, String> sensorState = new LinkedHashMap<>(sensor.getState());
    sensorState.remove("type");
    if (!sensorState.isEmpty()) {
      if (!sensorState.containsKey(reading)) {
        throw new IllegalArgumentException("Device " + sensorId + " does not report " + reading
            + ". It reports: " + sensorState.keySet());
      }
      try {
        Double.parseDouble(sensorState.get(reading));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Device " + sensorId + " reports " + reading
            + " as " + sensorState.get(reading) + ", which is not a number.");
      }
    }

    ControlledDevice device = requireControlled(deviceId);
    CommandSpec spec = device.getCommand(command).orElseThrow(() -> new IllegalArgumentException(
        "Device " + deviceId + " has not announced a command called " + command
            + ". Known commands: " + device.getCommands().keySet()));
    spec.validate(argument);
    String fullCommand = spec.getType() == ParameterType.NONE
        ? command
        : command + ":" + argument.trim();

    Automation automation =
        new Automation(sensorId, reading, condition, threshold, deviceId, fullCommand);
    String requested = automation.toRequestValue();
    CompletableFuture<String> answer =
        pending.add(Protocol.AUTOMATION_ADD, Protocol.AUTOMATION_ADDED, requested);
    sendMessage(MessageWorker.SERVER, Protocol.AUTOMATION_ADD.withValue(requested));
    // The reply is the automation in full form, with the id the server gave it.
    return answer.thenApply(reply -> Automation.fromFullValue(reply));
  }

  /**
   * Asks the server to remove an automation. {@link #getAutomations()} changes once the server
   * confirms.
   *
   * @param automationId the automation's id
   * @return completes when the server confirms, or fails with a {@link RequestFailedException}
   * @throws AccessDeniedException if the role is {@link Role#GUEST}
   * @throws IllegalArgumentException if no automation with this id is known
   */
  public CompletableFuture<Void> removeAutomation(long automationId) {
    require(Permission.MANAGE_AUTOMATIONS, "remove automations");
    synchronized (automations) {
      if (!automations.containsKey(automationId)) {
        throw new IllegalArgumentException("No automation with id " + automationId + ".");
      }
    }
    CompletableFuture<String> answer = pending.add(Protocol.AUTOMATION_REMOVE,
        Protocol.AUTOMATION_REMOVED, String.valueOf(automationId));
    sendMessage(MessageWorker.SERVER, Protocol.AUTOMATION_REMOVE.withValue(automationId));
    return answer.thenAccept(reply -> { });
  }

  /**
   * Returns every automation, as confirmed by the server. Every role may call this.
   *
   * @return a copy of the list, in the order they were added
   */
  public List<Automation> getAutomations() {
    synchronized (automations) {
      return new ArrayList<>(automations.values());
    }
  }

  /**
   * Returns every registered device in the home. Every role may call this.
   *
   * @return a copy of the list
   */
  public List<ControlledDevice> getDevices() {
    synchronized (devices) {
      return new ArrayList<>(devices.values());
    }
  }

  /**
   * Returns one controlled device. Every role may call this.
   *
   * @param deviceId the device's client id
   * @return the device, or empty if it is not registered
   */
  public Optional<ControlledDevice> getDevice(long deviceId) {
    return Optional.ofNullable(findDevice(deviceId));
  }

  /**
   * Sends a command to a registered device after checking it against the device's announced
   * command specification.
   *
   * @param deviceId the device's client id
   * @param command the command name, as announced by the device
   * @param argument the argument; ignored for commands without a parameter
   * @throws AccessDeniedException if the role may not control devices
   * @throws IllegalArgumentException if the device is not registered, has not announced this
   *     command, or the argument does not match the command's type or range
   */
  public void sendCommand(long deviceId, String command, String argument) {
    require(Permission.CONTROL_DEVICES, "control devices");
    ControlledDevice device = requireControlled(deviceId);
    CommandSpec spec = device.getCommand(command).orElseThrow(() -> new IllegalArgumentException(
        "Device " + deviceId + " has not announced a command called " + command
            + ". Known commands: " + device.getCommands().keySet()));
    spec.validate(argument);

    String message = spec.getType() == ParameterType.NONE
        ? command
        : command + ":" + argument.trim();
    sendMessage(address(deviceId), message);
  }

  /**
   * Asks a device for its available commands, and subscribes to its state. Done automatically
   * for every device the controller learns about.
   *
   * @param deviceId the device's client id
   */
  public void refresh(long deviceId) {
    sendMessage(address(deviceId), Protocol.COMMANDS_REQUEST.getKey());
    subscribe(deviceId);
  }

  /**
   * Subscribes to a registered device's state. The server sends the device's latest state at
   * once, and then every change, and {@link ControlledDevice#getState()} follows it.
   *
   * <p>The controller subscribes to every registered device by itself, so this is only needed
   * after {@link #unsubscribe(long)}.
   *
   * @param deviceId the device's client id
   */
  public void subscribe(long deviceId) {
    sendMessage(MessageWorker.SERVER, Protocol.SUBSCRIBE.withValue(deviceId));
  }

  /**
   * Stops receiving a device's state. {@link ControlledDevice#getState()} then keeps the last
   * values received, and stops changing.
   *
   * @param deviceId the device's client id
   */
  public void unsubscribe(long deviceId) {
    sendMessage(MessageWorker.SERVER, Protocol.UNSUBSCRIBE.withValue(deviceId));
  }

  /**
   * After a reconnect: subscribes again to every known device, and asks the server for the
   * home's devices, rooms and automations, in case something changed while the controller was
   * away. The controller keeps its id, because it introduces itself with the same name, but the
   * server forgets a client's subscriptions when its connection is lost.
   */
  @Override
  protected void onReconnected() {
    super.onReconnected();
    for (ControlledDevice device : getDevices()) {
      refresh(device.getId());
    }
    requestHomeState();
  }

  /**
   * Handles messages from the server (device, room and automation updates, and errors) and from
   * registered devices (command announcements, state updates and errors).
   *
   * @param message the raw message
   * @param sender the connection the message arrived on
   */
  @Override
  public void onMessage(String message, ClientConnectionHolder sender) {
    MessageWorker mw;
    try {
      mw = new MessageWorker(message);
    } catch (IllegalArgumentException e) {
      return;
    }
    String value = mw.getCommandValue();
    Protocol protocol = mw.getProtocol().orElse(null);

    if (protocol != null && handleCommonRequest(protocol, value, mw.getSource())) {
      return;
    }

    if ("SE".equals(mw.getSourcePrefix())) {
      if (protocol != null) {
        handleServerMessage(protocol, value);
        pending.onServerMessage(protocol, value);
      }
      return;
    }

    Long sourceId = mw.getSourceId();
    if (sourceId == null) {
      return;
    }
    ControlledDevice device = findDevice(sourceId);
    if (device == null) {
      return;
    }
    if (protocol != null) {
      handleDeviceProtocolMessage(device, protocol, value);
    } else {
      handleDeviceState(device, mw.getCommandKey(), value);
    }
  }

  /**
   * Handles a message received from the server and updates the local state of rooms, devices,
   * and automations accordingly.
   *
   * <p>Room messages add or remove a device from a room, and an empty room is discarded after
   * its last device is removed, unless it has a name. {@code ROOM-CREATED} and
   * {@code ROOM-RENAMED} add a room or set its name. Device messages add or remove a controlled device, and a newly
   * added device is refreshed. Automation messages add or remove an automation, and triggered
   * automations are ignored because the device reports its own new state. Device online and
   * offline messages update the availability of an existing device. Error messages are passed
   * to the pending request handler, or logged if no pending request accepts them. Unrecognized
   * protocols are ignored.
   *
   * @param protocol the protocol type of the message received from the server
   * @param value the payload of the message, whose format depends on the protocol type
   */
  private void handleServerMessage(Protocol protocol, String value) {
    switch (protocol) {
      case ROOM_ADDED, ROOM_REMOVED -> {
        long[] ids = parseRoomAndDevice(value);
        if (ids == null) {
          return;
        }
        synchronized (rooms) {
          if (protocol == Protocol.ROOM_ADDED) {
            rooms.computeIfAbsent(ids[0], id -> new LinkedHashSet<>()).add(ids[1]);
          } else {
            Set<Long> room = rooms.get(ids[0]);
            if (room != null) {
              room.remove(ids[1]);
              boolean named;
              synchronized (roomNames) {
                named = roomNames.containsKey(ids[0]);
              }
              if (room.isEmpty() && !named) {
                rooms.remove(ids[0]);
              }
            }
          }
        }
      }
      case ROOM_CREATED, ROOM_RENAMED -> {
        int comma = value.indexOf(',');
        long roomId;
        try {
          roomId = Long.parseLong(value.substring(0, Math.max(comma, 0)).trim());
        } catch (NumberFormatException e) {
          return;
        }
        String name = value.substring(comma + 1);
        synchronized (rooms) {
          rooms.computeIfAbsent(roomId, id -> new LinkedHashSet<>());
          synchronized (roomNames) {
            roomNames.put(roomId, name);
          }
        }
      }
      case DEVICE_ADDED, DEVICE_REMOVED -> {
        String[] parts = value.split(",", 3);
        long deviceId = firstNumber(value);
        if (deviceId < 0) {
          return;
        }
        if (protocol == Protocol.DEVICE_ADDED) {
          boolean isNew;
          ControlledDevice device;
          synchronized (devices) {
            isNew = !devices.containsKey(deviceId);
            device = devices.computeIfAbsent(deviceId, ControlledDevice::new);
          }
          if (parts.length == 3) {
            device.setName(parts[1]);
            device.setOnline(!parts[2].equals("OFFLINE"));
          }
          if (isNew) {
            refresh(deviceId);
          }
        } else {
          // The server has already removed every subscription to the device.
          synchronized (devices) {
            devices.remove(deviceId);
          }
        }
      }
      case AUTOMATION_ADDED -> {
        try {
          Automation automation = Automation.fromFullValue(value);
          synchronized (automations) {
            automations.put(automation.getId(), automation);
          }
        } catch (IllegalArgumentException e) {
          System.out.println("Server sent an invalid automation: " + value);
        }
      }
      case AUTOMATION_REMOVED -> {
        try {
          long id = Long.parseLong(value);
          synchronized (automations) {
            automations.remove(id);
          }
        } catch (NumberFormatException ignored) {
          // Malformed id; nothing to remove.
        }
      }
      case AUTOMATION_TRIGGERED -> {
        // The device's new state arrives from the device itself; nothing to store here.
      }
      case DEVICE_ONLINE, DEVICE_OFFLINE -> {
        ControlledDevice device = findDevice(firstNumber(value));
        if (device != null) {
          device.setOnline(protocol == Protocol.DEVICE_ONLINE);
        }
      }
      case ERROR -> {
        String[] parts = value.split(",", 2);
        String request = parts[0];
        String reason = parts.length == 2 ? parts[1] : value;
        if (!pending.onError(request, reason)) {
          System.out.println("Server rejected " + request + ": " + reason);
        }
      }
      default -> {
        // Not meant for the controller.
      }
    }
  }

  private void handleDeviceProtocolMessage(ControlledDevice device, Protocol protocol,
      String value) {
    switch (protocol) {
      case COMMAND -> {
        try {
          device.putCommand(CommandSpec.fromMessageValue(value));
        } catch (IllegalArgumentException e) {
          System.out.println("Device " + device.getId() + " sent an invalid command: " + value);
        }
      }
      case ERROR ->
          System.out.println("Device " + device.getId() + " rejected a command: " + value);
      default -> {
        // COMMAND_END and anything else: nothing to do.
      }
    }
  }

  /**
   * Stores a {@code key:value} state report from a device, such as {@code powerOn:true}.
   *
   * @param device the device that sent the report
   * @param key the property's name
   * @param value the property's value
   */
  private void handleDeviceState(ControlledDevice device, String key, String value) {
    if (key.equals("type")) {
      try {
        device.setType(DeviceType.valueOf(value));
      } catch (IllegalArgumentException ignored) {
        // Unknown type; leave it unset.
      }
      return;
    }
    device.putState(key, value);
  }

  /**
   * Checks that the controller has been started and that its role allows an action. Called
   * before anything else in a request, so a refused request leaves nothing behind, such as a
   * waiting future.
   */
  private void require(Permission permission, String action) {
    requireStarted();
    if (!role.has(permission)) {
      throw new AccessDeniedException("A " + role + " controller cannot " + action + ".");
    }
  }

  /**
   * Retrieves the controlled device with the given identifier.
   * If no device is registered under the specified identifier, an
   * {@link IllegalArgumentException} is thrown.
   *
   * @param deviceId the unique identifier of the device to retrieve
   * @return the {@link ControlledDevice} associated with the given identifier, never {@code null}
   * @throws IllegalArgumentException if no registered device exists with the given identifier
   */
  private ControlledDevice requireControlled(long deviceId) {
    ControlledDevice device = findDevice(deviceId);
    if (device == null) {
      throw new IllegalArgumentException(
          "Device " + deviceId + " is not a registered device.");
    }
    return device;
  }

  /**
   * Returns the number before the first comma of a value, for example 5 for
   * {@code 5,kitchen-light,ONLINE}.
   *
   * <p>Used for finding the id.
   *
   * @param value the value
   * @return the number, or -1 if there is none
   */
  private static long firstNumber(String value) {
    try {
      return Long.parseLong(value.split(",", 2)[0].trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Parses a string containing a room identifier and a device identifier separated by a comma.
   * Leading and trailing whitespace around each identifier is ignored.
   * If the input does not contain exactly two comma-separated parts, or if either part
   * cannot be parsed as a {@code long}, this method returns {@code null}.
   *
   * @param value the string to parse, expected in the form {@code "roomId,deviceId"}
   * @return an array of two {@code long} values where index {@code 0} holds the room identifier
   *         and index {@code 1} holds the device identifier, or {@code null} if the input is
   *         malformed or cannot be parsed
   */
  private static long[] parseRoomAndDevice(String value) {
    String[] parts = value.split(",");
    if (parts.length != 2) {
      return null;
    }
    try {
      return new long[] {Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim())};
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Checks a room name the same way the server does, so a bad name fails at once.
   *
   * @param name the name to check
   * @return the name without leading and trailing whitespace
   * @throws IllegalArgumentException if the name is blank or contains {@code /}, {@code =} or
   *     line breaks
   */
  private static String validateRoomName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Room name cannot be empty.");
    }
    String trimmed = name.trim();
    if (trimmed.chars().anyMatch(c -> "/=\n\r".indexOf(c) >= 0)) {
      throw new IllegalArgumentException("Room name cannot contain '/', '=' or line breaks.");
    }
    return trimmed;
  }

  private ControlledDevice findDevice(long deviceId) {
    synchronized (devices) {
      return devices.get(deviceId);
    }
  }

  private static String address(long deviceId) {
    return MessageWorker.address("CH", deviceId);
  }
}
