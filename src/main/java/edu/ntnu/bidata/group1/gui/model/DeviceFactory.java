package edu.ntnu.bidata.group1.gui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.device.actuator.DoorLock;
import edu.ntnu.bidata.group1.device.actuator.Fan;
import edu.ntnu.bidata.group1.device.actuator.Heater;
import edu.ntnu.bidata.group1.device.actuator.Light;
import edu.ntnu.bidata.group1.device.sensor.HumiditySensor;
import edu.ntnu.bidata.group1.device.sensor.Switch;
import edu.ntnu.bidata.group1.device.sensor.TemperatureSensor;

/**
 * Creates new simulated devices in this program and connects them to the server, so an owner
 * can add devices to the home from the GUI.
 *
 * <p>A created device is an ordinary client of the server, just like a device started on its
 * own. It is not part of the home until an owner registers it with
 * {@link edu.ntnu.bidata.group1.device.controller.Controller#addDevice(long)}, using the id
 * returned by {@link #create(Kind, String)}.
 *
 * <p>The devices keep running for as long as the program runs.
 *
 * <p>Creating a device with the name of a registered device that is offline brings that device
 * back: the server recognises the name, gives the device its old id, and keeps its
 * registration, rooms and automations. The server also remembers each device's kind, so the
 * right type to recreate it with is known; see {@link Kind#fromDeviceKind(String)}.
 *
 * <p>A {@link Switch} is a sensor that only changes when someone flips it. Since there is no
 * real switch to flip, {@link #flipSwitch(long)} does it for a switch created here.
 */
public final class DeviceFactory {

  /** How long to wait for the server to give a new device its id. */
  private static final long WELCOME_TIMEOUT_MS = 5000;

  /** The kinds of device that can be created. */
  public enum Kind {
    LIGHT("Light", Light.class, Light::new, Light::new),
    FAN("Fan", Fan.class, Fan::new, Fan::new),
    HEATER("Heater", Heater.class, Heater::new, Heater::new),
    DOOR_LOCK("Door lock", DoorLock.class, DoorLock::new, DoorLock::new),
    TEMPERATURE_SENSOR("Temperature sensor", TemperatureSensor.class, TemperatureSensor::new,
        TemperatureSensor::new),
    HUMIDITY_SENSOR("Humidity sensor", HumiditySensor.class, HumiditySensor::new,
        HumiditySensor::new),
    SWITCH("Switch", Switch.class, Switch::new, Switch::new);

    private final String label;
    private final Class<? extends Device> deviceClass;
    private final Function<String, Device> named;
    private final Supplier<Device> unnamed;

    Kind(String label, Class<? extends Device> deviceClass, Function<String, Device> named,
        Supplier<Device> unnamed) {
      this.label = label;
      this.deviceClass = deviceClass;
      this.named = named;
      this.unnamed = unnamed;
    }

    /**
     * Finds the kind for a device kind as the server reports it: the name of the device's
     * class, for example {@code Fan} or {@code DoorLock}. See
     * {@link edu.ntnu.bidata.group1.device.controller.ControlledDevice#getKind()}.
     *
     * @param deviceKind the reported kind
     * @return the kind, or empty if this program cannot create devices of that class
     */
    public static Optional<Kind> fromDeviceKind(String deviceKind) {
      for (Kind kind : values()) {
        if (kind.deviceClass.getSimpleName().equals(deviceKind)) {
          return Optional.of(kind);
        }
      }
      return Optional.empty();
    }

    /**
     * Returns the name shown to the user.
     *
     * @return for example {@code Door lock}
     */
    public String getLabel() {
      return label;
    }

    /**
     * Finds the kind with the given label.
     *
     * @param label the label, as returned by {@link #getLabel()}
     * @return the kind
     * @throws IllegalArgumentException if no kind has this label
     */
    public static Kind fromLabel(String label) {
      for (Kind kind : values()) {
        if (kind.label.equals(label)) {
          return kind;
        }
      }
      throw new IllegalArgumentException("Unknown device type: " + label);
    }
  }

  /** Keeps a reference to every created device. */
  private static final List<Device> created = new ArrayList<>();

  private DeviceFactory() {
  }

  /**
   * Creates a device, connects it to the server and waits for the id the server gives it.
   * Blocks for up to a few seconds, so call it from a background thread.
   *
   * @param kind the kind of device
   * @param name the device's name, for example {@code kitchen-light}; blank for a random name
   *     such as {@code light-3f9a1c2e}
   * @return the id the server gave the device, to use with
   *     {@link edu.ntnu.bidata.group1.device.controller.Controller#addDevice(long)}
   * @throws IllegalArgumentException if the name is not allowed, see
   *     {@link Device#checkName(String)}
   * @throws IOException if the server cannot be reached, or does not answer in time
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public static long create(Kind kind, String name) throws IOException, InterruptedException {
    if (kind == null) {
      throw new IllegalArgumentException("Choose a device type.");
    }
    Device device = name == null || name.isBlank()
        ? kind.unnamed.get()
        : kind.named.apply(name.trim());
    device.start();
    synchronized (created) {
      created.add(device);
    }
    long deadline = System.currentTimeMillis() + WELCOME_TIMEOUT_MS;
    while (device.getId() == 0) {
      if (System.currentTimeMillis() > deadline) {
        throw new IOException("The server did not give " + device.getName() + " an id.");
      }
      Thread.sleep(50);
    }
    return device.getId();
  }

  /**
   * Returns whether the device with this id is a {@link Switch} created by this program, so
   * {@link #flipSwitch(long)} can flip it.
   *
   * @param id the device's id
   * @return {@code true} if it is a switch created here
   */
  public static boolean isLocalSwitch(long id) {
    return findSwitch(id) != null;
  }

  /**
   * Flips a switch created by this program, as if someone pressed it. The switch reports its
   * new reading to the server like any other sensor.
   *
   * @param id the switch's id
   * @throws IllegalArgumentException if no switch with this id was created here
   */
  public static void flipSwitch(long id) {
    Switch device = findSwitch(id);
    if (device == null) {
      throw new IllegalArgumentException("Device " + id
          + " is not a switch running in this program, so it can only be flipped by hand.");
    }
    device.toggle();
  }

  private static Switch findSwitch(long id) {
    synchronized (created) {
      for (Device device : created) {
        if (device instanceof Switch found && device.getId() == id) {
          return found;
        }
      }
    }
    return null;
  }
}
