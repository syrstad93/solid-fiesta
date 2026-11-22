package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.utility.DeviceType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A picture of a device that changes with the device's state: a light glows, a fan spins, a
 * heater turns red, a lock opens.
 *
 * <p>The picture is chosen from the device's kind, as the server reports it, or from the state
 * values it reports if it has no kind; see {@link #kindOf(ControlledDevice)}.
 */
public abstract class DeviceVisual extends StackPane {

  /** The kinds of pictures there are. */
  public enum Kind {
    LIGHT, FAN, HEATER, DOOR_LOCK, SWITCH, SENSOR, GENERIC
  }

  /** Short text under the picture, for example {@code On · 75 %}. */
  protected final Label caption = new Label();

  /**
   * Creates the visual with the given picture above the caption.
   *
   * @param picture the picture
   */
  protected DeviceVisual(Node picture) {
    getStyleClass().add("device-visual");
    caption.getStyleClass().add("visual-caption");
    StackPane pictureBox = new StackPane(picture);
    pictureBox.setMinHeight(90);
    pictureBox.setPrefHeight(90);
    VBox box = new VBox(6, pictureBox, caption);
    box.setAlignment(Pos.CENTER);
    getChildren().add(box);
    setMinHeight(130);
  }

  /**
   * Shows the device's state.
   *
   * @param state the last state the device reported, for example {@code powerOn=true}
   * @param online whether the device is connected
   */
  public abstract void update(Map<String, String> state, boolean online);

  /**
   * Chooses the kind of picture for a device, from the state values it reports.
   *
   * @param device the device
   * @return the kind of picture
   */
  public static Kind kindOf(ControlledDevice device) {
    // The kind the server reports is certain, and known even while the device is offline.
    Kind reported = device.getKind().map(DeviceVisual::fromDeviceKind).orElse(null);
    if (reported != null) {
      return reported;
    }
    Map<String, String> state = device.getState();
    if (state.containsKey("locked")) {
      return Kind.DOOR_LOCK;
    }
    if (state.containsKey("speed")) {
      return Kind.FAN;
    }
    if (state.containsKey("targetTemperature")) {
      return Kind.HEATER;
    }
    if (state.containsKey("powerOn") && state.containsKey("value")) {
      return Kind.LIGHT;
    }
    boolean noCommands = device.getCommands().isEmpty();
    if (state.containsKey("on") && state.size() == 1 && noCommands) {
      return Kind.SWITCH;
    }
    boolean sensor = device.getType().map(type -> type == DeviceType.SENSOR).orElse(false);
    if (sensor || (device.getCommands().isEmpty() && !state.isEmpty())) {
      return Kind.SENSOR;
    }
    return Kind.GENERIC;
  }

  private static Kind fromDeviceKind(String deviceKind) {
    return switch (deviceKind) {
      case "Light" -> Kind.LIGHT;
      case "Fan" -> Kind.FAN;
      case "Heater" -> Kind.HEATER;
      case "DoorLock" -> Kind.DOOR_LOCK;
      case "Switch" -> Kind.SWITCH;
      case "TemperatureSensor", "HumiditySensor" -> Kind.SENSOR;
      default -> null;
    };
  }

  /**
   * Creates a picture of the given kind.
   *
   * @param kind the kind
   * @return a new visual
   */
  public static DeviceVisual create(Kind kind) {
    return switch (kind) {
      case LIGHT -> new LightVisual();
      case FAN -> new FanVisual();
      case HEATER -> new HeaterVisual();
      case DOOR_LOCK -> new DoorLockVisual();
      case SWITCH -> new SwitchVisual();
      case SENSOR -> new SensorVisual();
      case GENERIC -> new GenericVisual();
    };
  }

  /**
   * Reads a {@code true}/{@code false} state value.
   *
   * @param state the state
   * @param key the value's name
   * @return {@code true} only if the value is {@code true}
   */
  protected static boolean flag(Map<String, String> state, String key) {
    return Boolean.parseBoolean(state.get(key));
  }

  /**
   * Reads a number from the state.
   *
   * @param state the state
   * @param key the value's name
   * @param fallback returned if the value is missing or not a number
   * @return the number
   */
  protected static double number(Map<String, String> state, String key, double fallback) {
    try {
      return Double.parseDouble(state.get(key));
    } catch (NullPointerException | NumberFormatException e) {
      return fallback;
    }
  }
}
