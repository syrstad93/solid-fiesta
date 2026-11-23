package edu.ntnu.bidata.group1.gui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.utility.Automation;
import edu.ntnu.bidata.group1.utility.Permission;
import edu.ntnu.bidata.group1.utility.Role;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * The model in the GUI's model-view-controller design.
 *
 * <p>All data comes from a {@link Controller}: its devices, their state, the rooms and the
 * automations. The {@link Controller} updates itself from messages it receives from the server,
 * but has no way to tell anyone that something changed. This class fills that gap: it checks the
 * {@link Controller} a few times a second, on the JavaFX thread, and tells its listeners when
 * anything has changed, so views can redraw.
 *
 * <p>Views only read from this class. Changes are made by the GUI controllers, which call the
 * {@link Controller}'s methods directly.
 */
public class HomeModel {

  /** How often the {@link Controller} is checked for changes. */
  public static final Duration POLL_INTERVAL = Duration.millis(400);

  private final Controller controller;
  private final List<Runnable> listeners = new ArrayList<>();
  private final Timeline poller;
  private String lastSnapshot = "";

  /**
   * Creates a model on top of a started {@link Controller}.
   *
   * @param controller the controller the data comes from
   * @throws IllegalArgumentException if {@code controller} is null
   */
  public HomeModel(Controller controller) {
    if (controller == null) {
      throw new IllegalArgumentException("Controller cannot be null.");
    }
    this.controller = controller;
    this.poller = new Timeline(new KeyFrame(POLL_INTERVAL, event -> checkForChanges()));
    this.poller.setCycleCount(Animation.INDEFINITE);
  }

  /**
   * Returns the controller this model reads from. GUI controllers use it to make changes.
   *
   * @return the controller
   */
  public Controller getController() {
    return controller;
  }

  /**
   * Adds a listener that is run, on the JavaFX thread, whenever the data changes.
   *
   * @param listener the listener
   */
  public void addListener(Runnable listener) {
    listeners.add(listener);
  }

  /** Starts watching the controller for changes. Call on the JavaFX thread. */
  public void start() {
    checkForChanges();
    poller.play();
  }

  /** Stops watching the controller for changes. */
  public void stop() {
    poller.stop();
  }

  /**
   * Compares the controller's data with the last time it was checked, and tells the listeners
   * if anything changed. Called automatically; can also be called to update at once, for
   * example right after the server confirmed a request.
   */
  public void checkForChanges() {
    String snapshot = snapshot();
    if (!snapshot.equals(lastSnapshot)) {
      lastSnapshot = snapshot;
      for (Runnable listener : List.copyOf(listeners)) {
        listener.run();
      }
    }
  }

  private String snapshot() {
    StringBuilder builder = new StringBuilder();
    for (ControlledDevice device : controller.getDevices()) {
      builder.append(device).append('\n');
    }
    builder.append(controller.getRooms()).append('\n');
    builder.append(controller.getRoomNames()).append('\n');
    for (Automation automation : controller.getAutomations()) {
      builder.append(automation.toFullValue()).append('\n');
    }
    return builder.toString();
  }

  // ---- Read access for the views ----

  /**
   * Returns the controller's name.
   *
   * @return for example {@code anna-phone}
   */
  public String getName() {
    return controller.getName();
  }

  /**
   * Returns the controller's role.
   *
   * @return the role
   */
  public Role getRole() {
    return controller.getRole();
  }

  /**
   * Returns whether the controller's role allows the permission.
   *
   * @param permission the permission
   * @return {@code true} if allowed
   */
  public boolean can(Permission permission) {
    return controller.can(permission);
  }

  /**
   * Returns every registered device.
   *
   * @return the devices
   */
  public List<ControlledDevice> getDevices() {
    return controller.getDevices();
  }

  /**
   * Returns one registered device.
   *
   * @param deviceId the device's id
   * @return the device, or empty if it is not registered
   */
  public Optional<ControlledDevice> getDevice(long deviceId) {
    return controller.getDevice(deviceId);
  }

  /**
   * Returns the rooms and the ids of the devices in each.
   *
   * @return room id to device ids
   */
  public Map<Long, Set<Long>> getRooms() {
    return controller.getRooms();
  }

  /**
   * Returns every automation.
   *
   * @return the automations
   */
  public List<Automation> getAutomations() {
    return controller.getAutomations();
  }

  /**
   * Returns a name to show for a room.
   *
   * @param roomId the room's id
   * @return the room's name, or {@code Room <id>} if it has none
   */
  public String roomLabel(long roomId) {
    String name = controller.getRoomNames().get(roomId);
    return name == null || name.isBlank() ? "Room " + roomId : name;
  }

  /**
   * Returns a name to show for a device.
   *
   * @param deviceId the device's id
   * @return the device's name, or {@code Device <id>} if it is unknown
   */
  public String deviceLabel(long deviceId) {
    return controller.getDevice(deviceId)
        .map(ControlledDevice::getName)
        .filter(name -> !name.isBlank())
        .orElse("Device " + deviceId);
  }

  /**
   * Returns whether a device is a switch running in this program, which the user can flip.
   *
   * @param deviceId the device's id
   * @return {@code true} if it can be flipped from here
   */
  public boolean isLocalSwitch(long deviceId) {
    return DeviceFactory.isLocalSwitch(deviceId);
  }

  /**
   * Returns the ids of the rooms a device is in.
   *
   * @param deviceId the device's id
   * @return the room ids, in room order
   */
  public List<Long> roomsOf(long deviceId) {
    List<Long> result = new ArrayList<>();
    for (Map.Entry<Long, Set<Long>> room : controller.getRooms().entrySet()) {
      if (room.getValue().contains(deviceId)) {
        result.add(room.getKey());
      }
    }
    return result;
  }
}
