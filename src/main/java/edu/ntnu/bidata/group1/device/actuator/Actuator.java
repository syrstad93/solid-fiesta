package edu.ntnu.bidata.group1.device.actuator;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.device.DeviceType;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for devices that receive commands from controllers and change their state in response.
 *
 * <p>A subclass registers the commands it accepts with {@link #registerCommand(CommandSpec)}, usually
 * from its constructor, and implements {@link #executeCommand(String, String)} to carry them out.
 * Once the actuator is started, it announces its commands to all controllers. Incoming commands are
 * checked against their specifications before they reach the subclass, and any property changes
 * caused by a command are published to the server, so every subscribed controller sees them.
 *
 * <p>Actuators answer requests for their command list. Protocol replies and announcements are never
 * answered, which prevents two devices from endlessly sending errors to each other.
 */
public abstract class Actuator extends Device {

  private final Map<String, CommandSpec> commands = new LinkedHashMap<>();

  /**
   * Creates an actuator with the given name. The constructor registers the device with the
   * {@link DeviceType#ACTUATOR} type, so it is sent to controllers as {@code type:ACTUATOR}.
   * Subclasses should call {@link #registerCommand(CommandSpec)} from their constructors, before
   * {@link #start()} is called.
   *
   * @param name the actuator's name, for example {@code kitchen-light}; see
   *     {@link #checkName(String)}
   * @throws IllegalArgumentException if the name is not allowed
   */
  protected Actuator(String name) {
    super(name, DeviceType.ACTUATOR);
  }

  /**
   * Broadcasts this actuator's commands, so every controller learns them, and then sends its
   * whole state to the server. Runs once, when the actuator is started.
   */
  @Override
  protected void onStarted() {
    announceCommands(MessageWorker.BROADCAST);
    super.onStarted();
  }

  /**
   * Performs a command that has already been checked against its {@link CommandSpec}.
   *
   * <p>The argument is guaranteed to be valid for the command's parameter type and range, so
   * implementations can parse it directly. Update {@link #properties} to reflect the new state;
   * afterwards, every property that changed is sent to the server.
   *
   * @param name the command name, one of the registered commands
   * @param argument the validated argument, trimmed; empty for commands without a parameter
   */
  protected abstract void executeCommand(String name, String argument);

  /**
   * Adds a command this actuator accepts. Call this from the subclass constructor, before
   * {@link #start()}.
   *
   * @param spec the command specification
   * @throws IllegalArgumentException if {@code spec} is null or a command with the same name is
   *     already registered
   * @throws IllegalStateException if the actuator has already been started, because its
   *     commands have then been announced
   */
  protected final void registerCommand(CommandSpec spec) {
    if (spec == null) {
      throw new IllegalArgumentException("Command specification cannot be null.");
    }
    if (isStarted()) {
      throw new IllegalStateException("Commands must be registered before start().");
    }
    synchronized (commands) {
      if (commands.containsKey(spec.getName())) {
        throw new IllegalArgumentException("Command already registered: " + spec.getName());
      }
      commands.put(spec.getName(), spec);
    }
  }

  /**
   * Returns the commands this actuator accepts.
   *
   * @return a copy of the command specifications, in registration order
   */
  public final List<CommandSpec> getAvailableCommands() {
    synchronized (commands) {
      return new ArrayList<>(commands.values());
    }
  }

  /**
   * Sends every available command, then {@code COMMAND-END:<count>}, to the given destination.
   *
   * @param destination where to send the list, for example a controller's {@code CH<id>} or
   *     {@link MessageWorker#BROADCAST}
   */
  public final void announceCommands(String destination) {
    List<CommandSpec> specs = getAvailableCommands();
    for (CommandSpec spec : specs) {
      sendMessage(destination, Protocol.COMMAND.withValue(spec.toMessageValue()));
    }
    sendMessage(destination, Protocol.COMMAND_END.withValue(specs.size()));
  }

  @Override
  public final void onMessage(String message, ClientConnectionHolder sender) {
    MessageWorker mw;
    try {
      mw = new MessageWorker(message);
    } catch (IllegalArgumentException e) {
      return;
    }
    String replyTo = mw.getSource();
    String name = mw.getCommandKey();
    String argument = mw.getCommandValue();

    Optional<Protocol> protocol = mw.getProtocol();
    if (protocol.isPresent()) {
      handleProtocolMessage(protocol.get(), argument, replyTo);
      return;
    }

    CommandSpec spec;
    synchronized (commands) {
      spec = commands.get(name);
    }
    if (spec == null) {
      sendError(replyTo, name, "unknown command " + name);
      return;
    }
    try {
      spec.validate(argument);
    } catch (IllegalArgumentException e) {
      sendError(replyTo, name, e.getMessage());
      return;
    }

    Map<String, Object> before = new HashMap<>(properties);
    executeCommand(name, argument);
    // The server passes the change on to every subscriber, so all controllers see it, also when
    // the command came from one of the server's automations.
    publishChanges(before);
  }

  /**
   * Handles a message that is part of the protocol rather than a device command.
   *
   * <p>Replies and announcements, including this actuator's own broadcasts echoing back, are
   * never answered, so two devices cannot keep sending errors to each other. Requests meant for
   * the server are ignored too.
   *
   * @param protocol the protocol message received
   * @param argument the message's value
   * @param replyTo the sender's address
   */
  private void handleProtocolMessage(Protocol protocol, String argument, String replyTo) {
    if (handleCommonRequest(protocol, argument, replyTo)) {
      return;
    }
    if (protocol == Protocol.COMMANDS_REQUEST) {
      announceCommands(replyTo);
    }
    // Replies, announcements and server requests are not for actuators.
  }
}
