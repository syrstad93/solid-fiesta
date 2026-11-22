package edu.ntnu.bidata.group1.gui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.gui.model.HomeModel;
import edu.ntnu.bidata.group1.utility.Permission;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The Rooms tab: the rooms and the devices in each. Users and owners can create and rename
 * rooms and move devices in and out of them; guests can only look.
 */
public class RoomsView extends BorderPane {

  private final Label notice = new Label();
  private final TextField newRoomName = new TextField();
  private final Button createButton = new Button("Create room");
  private final ListView<Choice> roomList = new ListView<>();

  private final Label roomTitle = new Label("Select a room");
  private final TextField renameField = new TextField();
  private final Button renameButton = new Button("Rename");
  private final Button deleteButton = new Button("Delete room");
  private final ListView<Choice> devicesInRoom = new ListView<>();
  private final Button removeFromRoomButton = new Button("Remove from room");
  private final ComboBox<Choice> deviceToAdd = new ComboBox<>();
  private final Button addToRoomButton = new Button("Add to room");
  private final VBox details;

  private HomeModel model;
  private Long shownRoom;

  /** Creates the tab. */
  public RoomsView() {
    notice.getStyleClass().add("notice");
    notice.setWrapText(true);

    newRoomName.setPromptText("New room name, e.g. Kitchen");
    newRoomName.setOnAction(event -> createButton.fire());
    HBox.setHgrow(newRoomName, Priority.ALWAYS);
    HBox createBar = new HBox(8, newRoomName, createButton);
    createBar.setAlignment(Pos.CENTER_LEFT);

    roomList.setPlaceholder(new Label("No rooms yet"));
    roomList.setPrefWidth(260);
    roomList.getSelectionModel().selectedItemProperty().addListener((obs, old, now) ->
        showDetails());
    VBox left = new VBox(10, new Label("Rooms"), roomList, createBar);
    VBox.setVgrow(roomList, Priority.ALWAYS);
    left.getStyleClass().add("panel");

    roomTitle.getStyleClass().add("section-title");
    renameField.setPromptText("New name");
    renameField.setOnAction(event -> renameButton.fire());
    HBox.setHgrow(renameField, Priority.ALWAYS);
    deleteButton.getStyleClass().add("danger");
    HBox renameBar = new HBox(8, renameField, renameButton, deleteButton);
    devicesInRoom.setPlaceholder(new Label("No devices in this room"));
    deviceToAdd.setPromptText("Choose a device");
    deviceToAdd.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(deviceToAdd, Priority.ALWAYS);
    HBox addBar = new HBox(8, deviceToAdd, addToRoomButton);
    details = new VBox(10, roomTitle, renameBar, new Label("Devices in this room"),
        devicesInRoom, removeFromRoomButton, addBar);
    VBox.setVgrow(devicesInRoom, Priority.ALWAYS);
    details.getStyleClass().add("panel");
    HBox.setHgrow(details, Priority.ALWAYS);

    HBox content = new HBox(16, left, details);
    content.setPadding(new Insets(16));
    setTop(notice);
    BorderPane.setMargin(notice, new Insets(16, 16, 0, 16));
    setCenter(content);
  }

  /**
   * Sets what happens when the user creates a room.
   *
   * @param listener receives the name as typed
   */
  public void setOnCreateRoom(Consumer<String> listener) {
    createButton.setOnAction(event -> listener.accept(newRoomName.getText()));
  }

  /**
   * Sets what happens when the user renames the selected room.
   *
   * @param listener receives the room's id and the new name as typed
   */
  public void setOnRenameRoom(BiConsumer<Long, String> listener) {
    renameButton.setOnAction(event -> {
      if (shownRoom != null) {
        listener.accept(shownRoom, renameField.getText());
      }
    });
  }

  /**
   * Sets what happens when the user deletes the selected room.
   *
   * @param listener receives the room's id
   */
  public void setOnDeleteRoom(LongConsumer listener) {
    deleteButton.setOnAction(event -> {
      if (shownRoom != null) {
        listener.accept(shownRoom);
      }
    });
  }

  /**
   * Sets what happens when the user adds a device to the selected room.
   *
   * @param listener receives the room's id and the device's id, or {@code null} if no device
   *     is chosen
   */
  public void setOnAddToRoom(BiConsumer<Long, Long> listener) {
    addToRoomButton.setOnAction(event -> {
      if (shownRoom != null) {
        Choice device = deviceToAdd.getValue();
        listener.accept(shownRoom, device == null ? null : device.id());
      }
    });
  }

  /**
   * Sets what happens when the user takes a device out of the selected room.
   *
   * @param listener receives the room's id and the device's id, or {@code null} if no device
   *     is selected
   */
  public void setOnRemoveFromRoom(BiConsumer<Long, Long> listener) {
    removeFromRoomButton.setOnAction(event -> {
      if (shownRoom != null) {
        Choice device = devicesInRoom.getSelectionModel().getSelectedItem();
        listener.accept(shownRoom, device == null ? null : device.id());
      }
    });
  }

  /** Empties the field for a new room's name, after the room was created. */
  public void clearNewRoomName() {
    newRoomName.clear();
  }

  /**
   * Shows the model's rooms.
   *
   * @param model the model
   */
  public void render(HomeModel model) {
    this.model = model;
    boolean canManage = model.can(Permission.MANAGE_ROOMS);
    notice.setText("Your role (" + Texts.title(model.getRole())
        + ") can see rooms, but not change them. Users and owners can.");
    notice.setVisible(!canManage);
    notice.setManaged(!canManage);
    for (var node : List.of(newRoomName, createButton, renameField, renameButton, deleteButton,
        removeFromRoomButton, deviceToAdd, addToRoomButton)) {
      node.setDisable(!canManage);
    }

    List<Choice> rooms = new ArrayList<>();
    model.getRooms().forEach((roomId, deviceIds) -> rooms.add(new Choice(roomId,
        model.roomLabel(roomId) + "  (" + deviceIds.size()
            + (deviceIds.size() == 1 ? " device)" : " devices)"))));
    Choice.setItems(roomList, rooms);
    if (roomList.getSelectionModel().isEmpty() && !rooms.isEmpty()) {
      roomList.getSelectionModel().select(0);
    }
    showDetails();
  }

  private void showDetails() {
    Choice selected = roomList.getSelectionModel().getSelectedItem();
    details.setDisable(selected == null);
    if (model == null || selected == null) {
      shownRoom = null;
      roomTitle.setText("Select a room");
      return;
    }
    long roomId = selected.id();
    if (shownRoom == null || shownRoom != roomId) {
      renameField.setText(model.roomLabel(roomId));
    }
    shownRoom = roomId;
    roomTitle.setText(model.roomLabel(roomId));

    Set<Long> inRoom = model.getRooms().getOrDefault(roomId, Set.of());
    List<Choice> members = new ArrayList<>();
    List<Choice> others = new ArrayList<>();
    for (ControlledDevice device : model.getDevices()) {
      Choice choice = new Choice(device.getId(),
          model.deviceLabel(device.getId()) + "  #" + device.getId());
      (inRoom.contains(device.getId()) ? members : others).add(choice);
    }
    Choice.setItems(devicesInRoom, members);
    Choice.setItems(deviceToAdd, others);
  }
}
