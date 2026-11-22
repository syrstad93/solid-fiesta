package edu.ntnu.bidata.group1.device.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import edu.ntnu.bidata.group1.device.actuator.CommandSpec;
import edu.ntnu.bidata.group1.utility.DeviceType;

/**
 * A controller's record of one registered device: the device's id, name and type, whether it
 * is online, the commands it has announced, and the last state values it has reported.
 *
 * <p>This is the controller's view of a remote device, built from the messages that device sends.
 * It is not the device itself.
 */
public class ControlledDevice {

  private final long id;
  private String name = "";
  private boolean online = true;
  private DeviceType type;
  private String kind;
  private final Map<String, CommandSpec> commands = new LinkedHashMap<>();
  private final Map<String, String> state = new LinkedHashMap<>();

  /**
   * Creates an empty record for the device with the given id.
   *
   * @param id the device's client id on the server
   */
  public ControlledDevice(long id) {
    this.id = id;
  }

  /**
   * Returns the device's client id.
   *
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the device's name, as it introduced itself to the server.
   *
   * @return the name, for example {@code kitchen-light}; empty if not known yet
   */
  public synchronized String getName() {
    return name;
  }

  synchronized void setName(String name) {
    this.name = name;
  }

  /**
   * Returns whether the device is connected to the server. A registered device that loses its
   * connection stays registered, and is online again once it reconnects.
   *
   * @return {@code true} if the device is online
   */
  public synchronized boolean isOnline() {
    return online;
  }

  synchronized void setOnline(boolean online) {
    this.online = online;
  }

  /**
   * Returns the device type, once the device has reported it.
   *
   * @return the type, or empty if it is not known yet
   */
  public synchronized Optional<DeviceType> getType() {
    return Optional.ofNullable(type);
  }

  synchronized void setType(DeviceType type) {
    this.type = type;
  }

  /**
   * Returns what exactly the device is, as the server reported it: the name of the device's
   * class, for example {@code Fan}. Known even while the device is offline, because the server
   * remembers it.
   *
   * @return the kind, or empty if the device did not give one
   */
  public synchronized Optional<String> getKind() {
    return Optional.ofNullable(kind);
  }

  synchronized void setKind(String kind) {
    this.kind = kind;
  }

  /**
   * Returns the command with the given name, if the device has announced it.
   *
   * @param name the command name
   * @return the command specification, or empty if it is not known
   */
  public synchronized Optional<CommandSpec> getCommand(String name) {
    return Optional.ofNullable(commands.get(name));
  }

  /**
   * Returns all commands the device has announced.
   *
   * @return an unmodifiable copy, keyed by command name
   */
  public synchronized Map<String, CommandSpec> getCommands() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(commands));
  }

  synchronized void putCommand(CommandSpec spec) {
    commands.put(spec.getName(), spec);
  }

  synchronized void clearCommands() {
    commands.clear();
  }

  /**
   * Returns the last reported state values, such as {@code powerOn=true}.
   *
   * @return an unmodifiable copy of the state
   */
  public synchronized Map<String, String> getState() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(state));
  }

  synchronized void putState(String key, String value) {
    state.put(key, value);
  }

  @Override
  public synchronized String toString() {
    return "Device " + id + " " + name + " (" + (type == null ? "unknown type" : type)
        + (kind == null ? "" : " " + kind)
        + (online ? "" : ", offline") + ")"
        + " commands=" + commands.values() + " state=" + state;
  }
}
