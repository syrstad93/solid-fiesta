package edu.ntnu.bidata.group1.gui.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.ntnu.bidata.group1.device.controller.AccessDeniedException;
import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.gui.model.DeviceFactory;
import edu.ntnu.bidata.group1.gui.model.HomeModel;
import edu.ntnu.bidata.group1.gui.view.AutomationRequest;
import edu.ntnu.bidata.group1.gui.view.DashboardView;
import edu.ntnu.bidata.group1.gui.view.NewDeviceRequest;
import edu.ntnu.bidata.group1.utility.Permission;
import javafx.application.Platform;

/**
 * The GUI controller for the main window, in the model-view-controller design.
 *
 * <p>It connects the {@link DashboardView} to the device {@link Controller}: every button,
 * slider and form in the view ends up here, and is carried out by calling the matching
 * {@link Controller} method, such as {@link Controller#sendCommand(long, String, String)} or
 * {@link Controller#createRoom(String)}. When the {@link HomeModel} reports that something
 * changed, it asks the view to draw it again.
 *
 * <p>The {@link Controller} decides what the role may do. The view hides or disables what the
 * role cannot do, but if a request is refused anyway, the {@link AccessDeniedException} is
 * shown to the user like any other error.
 */
public class DashboardController {

  private final HomeModel model;
  private final Controller home;
  private final DashboardView view;

  /**
   * Connects the view to the model's controller.
   *
   * @param model the model
   * @param view the view
   */
  public DashboardController(HomeModel model, DashboardView view) {
    this.model = model;
    this.home = model.getController();
    this.view = view;

    view.setOnRefresh(this::refresh);

    view.getDevicesView().setOnCommand(this::sendCommand);
    List<String> deviceTypes = new ArrayList<>();
    for (DeviceFactory.Kind kind : DeviceFactory.Kind.values()) {
      deviceTypes.add(kind.getLabel());
    }
    view.getDevicesView().setDeviceTypes(deviceTypes);
    view.getDevicesView().setOnCreateDevice(this::createDevice);
    view.getDevicesView().setOnAddDevice(this::addDevice);
    view.getDevicesView().setOnRemoveDevice(this::removeDevice);
    view.getDevicesView().setOnFlipSwitch(this::flipSwitch);
    view.getDevicesView().setOnRestartDevice(this::restartDevice);

    view.getRoomsView().setOnCreateRoom(this::createRoom);
    view.getRoomsView().setOnRenameRoom(this::renameRoom);
    view.getRoomsView().setOnAddToRoom(this::addToRoom);
    view.getRoomsView().setOnRemoveFromRoom(this::removeFromRoom);

    view.getAutomationsView().setOnAddAutomation(this::addAutomation);
    view.getAutomationsView().setOnRemoveAutomation(this::removeAutomation);

    model.addListener(() -> view.render(model));
  }

  /** Shows the current data and starts following changes. */
  public void start() {
    view.render(model);
    model.start();
    view.showInfo("Connected as " + home.getName() + " with the " + home.getRole()
        + " role.");
  }

  /** Stops following changes, when the window is closed. */
  public void stop() {
    model.stop();
  }

  // ---- Devices ----

  private void refresh() {
    attempt(() -> {
      home.requestHomeState();
      for (ControlledDevice device : home.getDevices()) {
        home.refresh(device.getId());
      }
      view.showInfo("Asked the server and devices for fresh information.");
    });
  }

  private void sendCommand(long deviceId, String command, String argument) {
    attempt(() -> {
      home.sendCommand(deviceId, command, argument);
      String sent = argument.isEmpty() ? command : command + ":" + argument;
      view.showInfo("Sent " + sent + " to " + model.deviceLabel(deviceId) + ".");
    });
  }

  /**
   * Creates a new simulated device, connects it to the server, registers it with
   * {@link Controller#addDevice(long)}, and puts it in the chosen room with
   * {@link Controller#addDeviceToRoom(long, long)}.
   *
   * <p>If the name belongs to a registered device that is offline, the new device takes its
   * place instead: the server gives it the old id, and its registration, rooms and automations
   * still apply, so it is not registered again. A name that belongs to an online device is
   * refused, since the server would hand that device's connection over to the new one.
   */
  private void createDevice(NewDeviceRequest form) {
    if (!home.can(Permission.MANAGE_DEVICES)) {
      view.showError("A " + home.getRole() + " controller cannot create devices.");
      return;
    }
    if (form.type() == null) {
      view.showError("Choose a device type.");
      return;
    }
    String name = form.name() == null ? "" : form.name().trim();
    if (!name.isEmpty()) {
      try {
        Device.checkName(name);
      } catch (IllegalArgumentException e) {
        view.showError(e.getMessage());
        return;
      }
    }
    ControlledDevice existing = name.isEmpty() ? null : home.getDevices().stream()
        .filter(d -> d.getName().equals(name)).findFirst().orElse(null);
    if (existing != null && existing.isOnline()) {
      view.showError("There is already an online device called " + name
          + ". Choose another name.");
      return;
    }
    Long offlineId = existing == null ? null : existing.getId();
    DeviceFactory.Kind kind = DeviceFactory.Kind.fromLabel(form.type());
    Long roomId = form.room() == null ? null : form.room().id();
    view.getDevicesView().setCreatingDevice(true);
    view.showInfo(offlineId != null
        ? "Restarting " + name + " as a " + kind.getLabel().toLowerCase(Locale.ROOT) + "…"
        : "Creating a " + kind.getLabel().toLowerCase(Locale.ROOT)
            + " and connecting it to the server…");

    // Connecting takes a moment, so it is done off the JavaFX thread.
    Thread creator = new Thread(() -> {
      try {
        long deviceId = DeviceFactory.create(kind, name);
        Platform.runLater(() -> {
          if (offlineId != null && offlineId == deviceId) {
            finishReconnect(deviceId, roomId);
          } else {
            // A new name, or the server did not recognise the old one: register it as new.
            registerCreatedDevice(deviceId, roomId);
          }
        });
      } catch (IOException | IllegalArgumentException e) {
        Platform.runLater(() -> {
          view.getDevicesView().setCreatingDevice(false);
          view.showError("Could not create the device: " + e.getMessage());
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "create-device");
    creator.setDaemon(true);
    creator.start();
  }

  /**
   * Finishes bringing back an offline device. It is still registered, so only the room is
   * added, if one was chosen that the device is not already in.
   */
  private void finishReconnect(long deviceId, Long roomId) {
    view.getDevicesView().setCreatingDevice(false);
    view.getDevicesView().clearNewDeviceName();
    model.checkForChanges();
    String back = "Restarted " + model.deviceLabel(deviceId) + " (#" + deviceId
        + "). Its rooms and automations still apply";
    if (roomId == null || model.roomsOf(deviceId).contains(roomId)) {
      view.showInfo(back + ".");
    } else {
      request(() -> home.addDeviceToRoom(roomId, deviceId),
          done -> back + ", and it is now also in " + model.roomLabel(roomId) + ".");
    }
  }

  /**
   * Restarts an offline device from its card. The server remembers what kind of device it was,
   * so a new device of the same kind is started with its name, and takes its place. A device
   * that did not report its kind cannot be restarted this way: the New device form is filled
   * in with its name, for the owner to choose the type.
   */
  private void restartDevice(long deviceId) {
    ControlledDevice device = home.getDevice(deviceId).orElse(null);
    if (device == null || device.getName().isBlank()) {
      view.showError("Device " + deviceId + " is not known by name, so it cannot be restarted.");
      return;
    }
    String name = device.getName();
    DeviceFactory.Kind kind = device.getKind().flatMap(DeviceFactory.Kind::fromDeviceKind)
        .orElse(null);
    if (kind != null) {
      createDevice(new NewDeviceRequest(kind.getLabel(), name, null));
      return;
    }
    view.getDevicesView().prefillNewDevice(name, null);
    view.showInfo(device.getKind()
        .map(k -> name + " is a " + k + ", which this program cannot create.")
        .orElse(name + " did not tell the server what kind of device it is.")
        + " Choose a type in the New device form above, then click Create device.");
  }

  private void registerCreatedDevice(long deviceId, Long roomId) {
    CompletableFuture<ControlledDevice> added;
    try {
      added = home.addDevice(deviceId);
    } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
      view.getDevicesView().setCreatingDevice(false);
      view.showError(e.getMessage());
      return;
    }
    added.whenComplete((device, error) -> Platform.runLater(() -> {
      view.getDevicesView().setCreatingDevice(false);
      if (error != null) {
        view.showError("The device was created, but could not be registered: "
            + reason(error));
        return;
      }
      view.getDevicesView().clearNewDeviceName();
      model.checkForChanges();
      String created = "Created " + model.deviceLabel(deviceId) + " (#" + deviceId + ")";
      if (roomId == null) {
        view.showInfo(created + ".");
      } else {
        request(() -> home.addDeviceToRoom(roomId, deviceId),
            done -> created + " in " + model.roomLabel(roomId) + ".");
      }
    }));
  }

  /**
   * Flips a switch created in this program, as if someone pressed it. A switch is a sensor and
   * takes no commands, so this does not go through the {@link Controller}; the controller sees
   * the switch's new reading once the switch reports it.
   */
  private void flipSwitch(long deviceId) {
    attempt(() -> {
      DeviceFactory.flipSwitch(deviceId);
      view.showInfo("Flipped " + model.deviceLabel(deviceId) + ".");
    });
  }

  private void addDevice(String typedId) {
    Long deviceId = parseId(typedId, "device id");
    if (deviceId == null) {
      return;
    }
    request(() -> home.addDevice(deviceId), device -> {
      view.getDevicesView().clearNewDeviceId();
      return "Added " + (device == null ? "device " + deviceId : device.getName())
          + " (#" + deviceId + ").";
    });
  }

  private void removeDevice(long deviceId) {
    String label = model.deviceLabel(deviceId);
    if (view.confirm("Remove " + label + " from the home? It is also taken out of every "
        + "room, and its automations are deleted.")) {
      request(() -> home.removeDevice(deviceId), done -> "Removed " + label + ".");
    }
  }

  // ---- Rooms ----

  private void createRoom(String name) {
    request(() -> home.createRoom(name), roomId -> {
      view.getRoomsView().clearNewRoomName();
      return "Created room " + name.trim() + ".";
    });
  }

  private void renameRoom(long roomId, String name) {
    request(() -> home.renameRoom(roomId, name), done -> "Renamed the room to " + name.trim()
        + ".");
  }

  private void addToRoom(long roomId, Long deviceId) {
    if (deviceId == null) {
      view.showError("Choose a device to add to the room.");
      return;
    }
    request(() -> home.addDeviceToRoom(roomId, deviceId), done ->
        "Put " + model.deviceLabel(deviceId) + " in " + model.roomLabel(roomId) + ".");
  }

  private void removeFromRoom(long roomId, Long deviceId) {
    if (deviceId == null) {
      view.showError("Select a device in the room first.");
      return;
    }
    request(() -> home.removeDeviceFromRoom(roomId, deviceId), done ->
        "Took " + model.deviceLabel(deviceId) + " out of " + model.roomLabel(roomId) + ".");
  }

  // ---- Automations ----

  private void addAutomation(AutomationRequest form) {
    if (form.sensor() == null || form.device() == null) {
      view.showError("Choose both the device to watch and the device to control.");
      return;
    }
    if (form.reading() == null || form.reading().isBlank()) {
      view.showError("Choose the reading to watch, for example temperature.");
      return;
    }
    if (form.command() == null || form.condition() == null) {
      view.showError("Choose a condition and a command.");
      return;
    }
    double threshold;
    try {
      threshold = Double.parseDouble(form.threshold().trim().replace(',', '.'));
    } catch (NumberFormatException e) {
      view.showError("The threshold must be a number, for example 22.5.");
      return;
    }
    request(() -> home.addAutomation(form.sensor().id(), form.reading().trim(),
            form.condition(), threshold, form.device().id(), form.command(),
            form.argument() == null ? "" : form.argument()),
        automation -> "Added automation #" + automation.getId() + ".");
  }

  private void removeAutomation(long automationId) {
    request(() -> home.removeAutomation(automationId),
        done -> "Removed automation #" + automationId + ".");
  }

  // ---- Helpers ----

  /**
   * Runs something that calls the {@link Controller} directly, and shows any refusal as an
   * error.
   */
  private void attempt(Runnable action) {
    try {
      action.run();
    } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
      view.showError(e.getMessage());
    }
  }

  /**
   * Sends a request that the server answers later. Shows the success message, or the reason
   * the request was refused, once the answer arrives. The answer arrives on a network thread,
   * so the view is updated on the JavaFX thread.
   */
  private <T> void request(Supplier<CompletableFuture<T>> send, Function<T, String> success) {
    CompletableFuture<T> answer;
    try {
      answer = send.get();
    } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
      view.showError(e.getMessage());
      return;
    }
    view.showInfo("Waiting for the server…");
    answer.whenComplete((result, error) -> Platform.runLater(() -> {
      if (error != null) {
        view.showError("Refused: " + reason(error));
      } else {
        model.checkForChanges();
        view.showInfo(success.apply(result));
      }
    }));
  }

  private Long parseId(String text, String what) {
    try {
      long id = Long.parseLong(text == null ? "" : text.trim());
      if (id < 0) {
        throw new NumberFormatException();
      }
      return id;
    } catch (NumberFormatException e) {
      view.showError("The " + what + " must be a whole number, for example 6.");
      return null;
    }
  }

  private static String reason(Throwable error) {
    Throwable cause = error instanceof CompletionException && error.getCause() != null
        ? error.getCause() : error;
    String message = cause.getMessage();
    return message == null ? cause.getClass().getSimpleName().toLowerCase(Locale.ROOT)
        : message;
  }
}
