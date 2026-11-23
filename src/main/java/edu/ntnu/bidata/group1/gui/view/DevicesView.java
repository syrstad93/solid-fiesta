package edu.ntnu.bidata.group1.gui.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.gui.model.HomeModel;
import edu.ntnu.bidata.group1.utility.Permission;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The Devices tab: a card for every registered device, a filter to show one room, and for
 * owners a field to register a new device by its id.
 */
public class DevicesView extends BorderPane {

  private static final long ALL = -1;
  private static final long NO_ROOM = -2;

  private final ComboBox<Choice> roomFilter = new ComboBox<>();
  private final TextField newDeviceId = new TextField();
  private final Button addDeviceButton = new Button("Add device");
  private final ComboBox<String> newDeviceType = new ComboBox<>();
  private final TextField newDeviceName = new TextField();
  private final ComboBox<Choice> newDeviceRoom = new ComboBox<>();
  private final Button createDeviceButton = new Button("Create device");
  private final VBox ownerBars;
  private final FlowPane cards = new FlowPane(16, 16);
  private final Label empty = new Label();
  private final Map<Long, DeviceCard> cardsById = new LinkedHashMap<>();

  private DeviceCard.CommandListener commandListener;
  private LongConsumer removeListener;
  private LongConsumer flipListener;
  private LongConsumer restartListener;

  /** Creates the tab. */
  public DevicesView() {
    roomFilter.setValue(new Choice(ALL, "All devices"));
    roomFilter.setOnAction(event -> refilter());

    newDeviceId.setPromptText("Client id, e.g. 6");
    newDeviceId.setPrefColumnCount(10);
    newDeviceId.setOnAction(event -> addDeviceButton.fire());
    HBox registerBar = new HBox(8, new Label("…or register a client that is already "
        + "connected:"), newDeviceId, addDeviceButton);
    registerBar.setAlignment(Pos.CENTER_LEFT);

    newDeviceType.setPromptText("Device type");
    newDeviceName.setPromptText("Name, e.g. kitchen-light (optional)");
    newDeviceName.setPrefColumnCount(18);
    newDeviceName.setOnAction(event -> createDeviceButton.fire());
    newDeviceRoom.setPromptText("Room (optional)");
    createDeviceButton.getStyleClass().add("primary");
    HBox createBar = new HBox(8, new Label("New device:"), newDeviceType, newDeviceName,
        newDeviceRoom, createDeviceButton);
    createBar.setAlignment(Pos.CENTER_LEFT);

    ownerBars = new VBox(8, createBar, registerBar);
    ownerBars.getStyleClass().add("owner-bars");

    HBox filterBar = new HBox(10, new Label("Show:"), roomFilter);
    filterBar.setAlignment(Pos.CENTER_LEFT);
    VBox toolbar = new VBox(10, ownerBars, filterBar);
    toolbar.getStyleClass().add("toolbar");
    setTop(toolbar);

    cards.setPadding(new Insets(16));
    empty.getStyleClass().add("muted");
    ScrollPane scroll = new ScrollPane(cards);
    scroll.setFitToWidth(true);
    scroll.getStyleClass().add("cards-scroll");
    setCenter(scroll);
  }

  /**
   * Sets who receives commands from the device cards.
   *
   * @param listener the listener
   */
  public void setOnCommand(DeviceCard.CommandListener listener) {
    this.commandListener = listener;
    cardsById.values().forEach(card -> card.setOnCommand(listener));
  }

  /**
   * Sets what happens when the user registers a device. Receives the id as typed.
   *
   * @param listener the listener
   */
  public void setOnAddDevice(Consumer<String> listener) {
    addDeviceButton.setOnAction(event -> listener.accept(newDeviceId.getText()));
  }

  /**
   * Sets what happens when the user removes a device.
   *
   * @param listener receives the device's id
   */
  public void setOnRemoveDevice(LongConsumer listener) {
    this.removeListener = listener;
    cardsById.values().forEach(card -> card.setOnRemove(listener));
  }

  /**
   * Sets the device types an owner can create.
   *
   * @param types the types, as shown to the user
   */
  public void setDeviceTypes(List<String> types) {
    newDeviceType.getItems().setAll(types);
    if (!types.isEmpty()) {
      newDeviceType.setValue(types.get(0));
    }
  }

  /**
   * Sets what happens when the owner creates a device.
   *
   * @param listener receives what the owner filled in
   */
  public void setOnCreateDevice(Consumer<NewDeviceRequest> listener) {
    createDeviceButton.setOnAction(event -> {
      Choice room = newDeviceRoom.getValue();
      listener.accept(new NewDeviceRequest(newDeviceType.getValue(), newDeviceName.getText(),
          room == null || room.id() == NO_ROOM ? null : room));
    });
  }

  /**
   * Shows that a device is being created, and stops the owner from creating another at the
   * same time.
   *
   * @param busy whether a device is being created
   */
  public void setCreatingDevice(boolean busy) {
    createDeviceButton.setDisable(busy);
    createDeviceButton.setText(busy ? "Creating…" : "Create device");
  }

  /** Empties the new device's name, after it was created. */
  public void clearNewDeviceName() {
    newDeviceName.clear();
  }

  /**
   * Sets what happens when the user flips a switch that runs in this program.
   *
   * @param listener receives the switch's id
   */
  public void setOnFlipSwitch(LongConsumer listener) {
    this.flipListener = listener;
  }

  /**
   * Sets what happens when the owner clicks Restart device on an offline device.
   *
   * @param listener receives the device's id
   */
  public void setOnRestartDevice(LongConsumer listener) {
    this.restartListener = listener;
  }

  /**
   * Fills in the New device form, so the owner only has to check the type and click Create
   * device. Used to restart an offline device whose type is not known.
   *
   * @param name the device's name
   * @param type the suggested type, or {@code null} to let the owner choose
   */
  public void prefillNewDevice(String name, String type) {
    newDeviceName.setText(name);
    newDeviceRoom.setValue(null);
    if (type != null) {
      newDeviceType.setValue(type);
    }
    newDeviceType.requestFocus();
  }

  /** Empties the field for a new device's id, after it was added. */
  public void clearNewDeviceId() {
    newDeviceId.clear();
  }

  /**
   * Shows the model's devices.
   *
   * @param model the model
   */
  public void render(HomeModel model) {
    boolean canManage = model.can(Permission.MANAGE_DEVICES);
    ownerBars.setVisible(canManage);
    ownerBars.setManaged(canManage);

    List<Choice> filters = new ArrayList<>();
    filters.add(new Choice(ALL, "All devices"));
    filters.add(new Choice(NO_ROOM, "Not in a room"));
    for (long roomId : model.getRooms().keySet()) {
      filters.add(new Choice(roomId, model.roomLabel(roomId)));
    }
    Choice.setItems(roomFilter, filters);
    List<Choice> roomChoices = new ArrayList<>();
    roomChoices.add(new Choice(NO_ROOM, "No room"));
    roomChoices.addAll(filters.subList(2, filters.size()));
    Choice.setItems(newDeviceRoom, roomChoices);
    if (roomFilter.getValue() == null) {
      roomFilter.setValue(filters.get(0));
    }

    List<ControlledDevice> devices = model.getDevices();
    List<Long> present = new ArrayList<>();
    for (ControlledDevice device : devices) {
      present.add(device.getId());
      DeviceCard card = cardsById.computeIfAbsent(device.getId(), this::newCard);
      List<Long> roomIds = model.roomsOf(device.getId());
      List<String> roomNames = new ArrayList<>();
      roomIds.forEach(id -> roomNames.add(model.roomLabel(id)));
      String roomText = roomNames.isEmpty() ? "Not in a room" : String.join(", ", roomNames);
      String kind = device.getKind().map(Texts::pretty).orElse(null);
      if (kind != null) {
        roomText = kind + " · " + roomText;
      }
      card.update(device, roomText, model.can(Permission.CONTROL_DEVICES), canManage);
      card.setFlippable(model.isLocalSwitch(device.getId()));
      card.getProperties().put("rooms", roomIds);
    }
    cardsById.keySet().retainAll(present);
    empty.setText(devices.isEmpty()
        ? (canManage
            ? "No devices registered yet. Create one above."
            : "No devices registered yet. Ask an owner to add some.")
        : "No devices in this room.");
    refilter();
  }

  private DeviceCard newCard(long deviceId) {
    DeviceCard card = new DeviceCard(deviceId);
    card.setOnCommand((id, command, argument) -> {
      if (commandListener != null) {
        commandListener.onCommand(id, command, argument);
      }
    });
    card.setOnFlip(id -> {
      if (flipListener != null) {
        flipListener.accept(id);
      }
    });
    card.setOnRestart(id -> {
      if (restartListener != null) {
        restartListener.accept(id);
      }
    });
    card.setOnRemove(id -> {
      if (removeListener != null) {
        removeListener.accept(id);
      }
    });
    return card;
  }

  @SuppressWarnings("unchecked")
  private void refilter() {
    Choice filter = roomFilter.getValue();
    long wanted = filter == null ? ALL : filter.id();
    List<Node> shown = new ArrayList<>();
    for (DeviceCard card : cardsById.values()) {
      List<Long> roomIds = (List<Long>) card.getProperties().getOrDefault("rooms", List.of());
      boolean show = wanted == ALL
          || (wanted == NO_ROOM && roomIds.isEmpty())
          || roomIds.contains(wanted);
      if (show) {
        shown.add(card);
      }
    }
    if (shown.isEmpty()) {
      shown.add(empty);
    }
    if (!cards.getChildren().equals(shown)) {
      cards.getChildren().setAll(shown);
    }
  }
}
