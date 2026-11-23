package edu.ntnu.bidata.group1.gui.view;

import edu.ntnu.bidata.group1.gui.model.HomeModel;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The main window for one connected controller: a header with the controller's name and role,
 * tabs for devices, rooms and automations, and a status line at the bottom.
 */
public class DashboardView extends BorderPane {

  private final Label name = new Label();
  private final Label role = new Label();
  private final Label permissions = new Label();
  private final Button refreshButton = new Button("Refresh");
  private final Label status = new Label("Connected.");

  private final DevicesView devicesView = new DevicesView();
  private final RoomsView roomsView = new RoomsView();
  private final AutomationsView automationsView = new AutomationsView();

  /** Creates the window's content. */
  public DashboardView() {
    getStyleClass().add("dashboard");

    name.getStyleClass().add("app-title");
    role.getStyleClass().add("role-badge");
    permissions.getStyleClass().add("muted");
    VBox titles = new VBox(2, new HBox(10, name, role), permissions);
    ((HBox) titles.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox header = new HBox(12, titles, spacer, refreshButton);
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add("header");
    setTop(header);

    TabPane tabs = new TabPane(
        new Tab("Devices", devicesView),
        new Tab("Rooms", roomsView),
        new Tab("Automations", automationsView));
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    setCenter(tabs);

    status.getStyleClass().add("status-bar");
    status.setMaxWidth(Double.MAX_VALUE);
    setBottom(status);
  }

  /**
   * Returns the Devices tab.
   *
   * @return the tab's view
   */
  public DevicesView getDevicesView() {
    return devicesView;
  }

  /**
   * Returns the Rooms tab.
   *
   * @return the tab's view
   */
  public RoomsView getRoomsView() {
    return roomsView;
  }

  /**
   * Returns the Automations tab.
   *
   * @return the tab's view
   */
  public AutomationsView getAutomationsView() {
    return automationsView;
  }

  /**
   * Sets what happens when the user clicks Refresh.
   *
   * @param listener the action
   */
  public void setOnRefresh(Runnable listener) {
    refreshButton.setOnAction(event -> listener.run());
  }

  /**
   * Shows everything in the model.
   *
   * @param model the model
   */
  public void render(HomeModel model) {
    name.setText(model.getName());
    role.setText(model.getRole().name());
    role.getStyleClass().removeAll("GUEST", "USER", "OWNER");
    role.getStyleClass().add(model.getRole().name());
    permissions.setText("Can: " + Texts.describe(model.getRole()));
    devicesView.render(model);
    roomsView.render(model);
    automationsView.render(model);
  }

  /**
   * Shows a message in the status line.
   *
   * @param message the message
   */
  public void showInfo(String message) {
    status.getStyleClass().remove("error");
    status.setText(message);
  }

  /**
   * Shows an error in the status line.
   *
   * @param message what went wrong
   */
  public void showError(String message) {
    if (!status.getStyleClass().contains("error")) {
      status.getStyleClass().add("error");
    }
    status.setText(message);
  }

  /**
   * Asks the user to confirm something.
   *
   * @param question the question
   * @return {@code true} if the user clicked OK
   */
  public boolean confirm(String question) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK,
        ButtonType.CANCEL);
    alert.setHeaderText(null);
    if (getScene() != null) {
      alert.initOwner(getScene().getWindow());
    }
    return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
  }
}
