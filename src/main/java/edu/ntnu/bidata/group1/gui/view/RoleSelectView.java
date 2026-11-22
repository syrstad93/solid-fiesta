package edu.ntnu.bidata.group1.gui.view;

import edu.ntnu.bidata.group1.utility.Role;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The first window: the user picks a role, optionally a name, and connects a controller. Can
 * be used several times to open controllers with different roles side by side.
 */
public class RoleSelectView extends VBox {

  private final TextField nameField = new TextField();
  private final ToggleGroup roles = new ToggleGroup();
  private final Button connectButton = new Button("Connect");
  private final ProgressIndicator progress = new ProgressIndicator();
  private final Label status = new Label();
  private final Button demoButton = new Button("Start demo home");
  private final Label demoStatus = new Label();

  /** Creates the window's content. */
  public RoleSelectView() {
    getStyleClass().add("role-select");
    setSpacing(16);
    setPadding(new Insets(28));

    Label title = new Label("Smart Home");
    title.getStyleClass().add("app-title");
    Label subtitle = new Label("Choose your role to connect a controller.");
    subtitle.getStyleClass().add("muted");

    HBox roleCards = new HBox(12);
    for (Role role : Role.values()) {
      roleCards.getChildren().add(roleCard(role));
    }
    roles.getToggles().get(1).setSelected(true);
    // Keep one role selected: clicking the selected card again does not clear it.
    roles.selectedToggleProperty().addListener((obs, old, now) -> {
      if (now == null && old != null) {
        old.setSelected(true);
      }
    });

    nameField.setPromptText("Optional, e.g. anna-phone (a random name is used if empty)");
    nameField.setOnAction(event -> connectButton.fire());
    Label nameLabel = new Label("Controller name");

    connectButton.setDefaultButton(true);
    connectButton.getStyleClass().add("primary");
    progress.setPrefSize(22, 22);
    progress.setVisible(false);
    HBox connectRow = new HBox(12, connectButton, progress);
    connectRow.setAlignment(Pos.CENTER_LEFT);
    status.setWrapText(true);

    Label server = new Label("Server: " + UniversallyAvailableInformation.LOCALHOST + ":"
        + UniversallyAvailableInformation.PORT_NUMBER);
    server.getStyleClass().add("muted");

    Label demoTitle = new Label("No server running?");
    demoTitle.getStyleClass().add("section-title");
    Label demoText = new Label("Start a server with simulated devices (lights, fan, heater, "
        + "door lock, thermometer) in this program, then connect.");
    demoText.setWrapText(true);
    demoText.getStyleClass().add("muted");
    demoStatus.setWrapText(true);

    getChildren().addAll(title, subtitle, roleCards, nameLabel, nameField, connectRow, status,
        server, new Separator(), demoTitle, demoText, demoButton, demoStatus);
  }

  private ToggleButton roleCard(Role role) {
    Label name = new Label(Texts.title(role));
    name.getStyleClass().add("role-name");
    Label can = new Label(Texts.describe(role).replace(" · ", "\n"));
    can.setWrapText(true);
    can.getStyleClass().add("role-description");
    VBox content = new VBox(6, name, can);
    ToggleButton card = new ToggleButton();
    card.setGraphic(content);
    card.setUserData(role);
    card.setToggleGroup(roles);
    card.getStyleClass().add("role-card");
    card.setMaxWidth(Double.MAX_VALUE);
    card.setAlignment(Pos.TOP_LEFT);
    HBox.setHgrow(card, Priority.ALWAYS);
    return card;
  }

  /**
   * Returns the chosen role.
   *
   * @return the role
   */
  public Role getSelectedRole() {
    return (Role) roles.getSelectedToggle().getUserData();
  }

  /**
   * Returns the name as typed.
   *
   * @return the name, possibly empty
   */
  public String getControllerName() {
    return nameField.getText();
  }

  /**
   * Sets what happens when the user clicks Connect.
   *
   * @param listener the action
   */
  public void setOnConnect(Runnable listener) {
    connectButton.setOnAction(event -> listener.run());
  }

  /**
   * Sets what happens when the user clicks Start demo home.
   *
   * @param listener the action
   */
  public void setOnStartDemo(Runnable listener) {
    demoButton.setOnAction(event -> listener.run());
  }

  /**
   * Shows that a connection is being made, and stops the user from starting another.
   *
   * @param busy whether a connection is being made
   */
  public void setConnecting(boolean busy) {
    connectButton.setDisable(busy);
    progress.setVisible(busy);
  }

  /**
   * Shows a message under the Connect button.
   *
   * @param message the message
   * @param error whether it is an error
   */
  public void showStatus(String message, boolean error) {
    status.setText(message);
    status.getStyleClass().remove("error");
    if (error) {
      status.getStyleClass().add("error");
    }
  }

  /**
   * Shows a message about the demo home, and whether it can still be started.
   *
   * @param message the message
   * @param canStart whether the Start demo home button should be enabled
   */
  public void showDemoStatus(String message, boolean canStart) {
    demoStatus.setText(message);
    demoButton.setDisable(!canStart);
  }

  /** Empties the name field, so the next controller gets a new name. */
  public void clearName() {
    nameField.clear();
  }
}
