package edu.ntnu.bidata.group1.gui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import edu.ntnu.bidata.group1.device.actuator.CommandSpec;
import edu.ntnu.bidata.group1.device.actuator.ParameterType;
import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.gui.view.visual.DeviceVisual;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * A card showing one device: its picture, name, status and rooms, and a control for every
 * command the device has announced.
 *
 * <p>The controls are built from the device's {@link CommandSpec}s, so any actuator gets
 * working controls: a switch for {@code BOOLEAN}, a slider for {@code INTEGER} and
 * {@code DECIMAL}, buttons for {@code ENUM} options and {@code NONE} commands. Using a control
 * only reports the command to the listener; the card changes once the device reports its new
 * state.
 */
public class DeviceCard extends VBox {

  /** Receives the commands the user gives with the card's controls. */
  public interface CommandListener {
    /**
     * Called when the user uses a control.
     *
     * @param deviceId the device's id
     * @param command the command's name, for example {@code power}
     * @param argument the argument, for example {@code true}; empty for commands without one
     */
    void onCommand(long deviceId, String command, String argument);
  }

  private static final long USER_EDIT_GRACE_MS = 1500;

  private final long deviceId;
  private final Label title = new Label();
  private final Label status = new Label();
  private final Label rooms = new Label();
  private final StackPane visualHolder = new StackPane();
  private final VBox controls = new VBox(8);
  private final Button flipButton = new Button("Flip switch");
  private final Button restartButton = new Button("Restart device");
  private final Button removeButton = new Button("Remove device");
  private final List<Consumer<Map<String, String>>> controlUpdaters = new ArrayList<>();

  private DeviceVisual visual;
  private DeviceVisual.Kind kind;
  private String commandsShown;
  private CommandListener commandListener;
  private long lastUserEdit;
  private Map<String, String> lastState = Map.of();
  /** Shows the latest state once the user has stopped using the controls; see {@link #send}. */
  private final PauseTransition afterUserEdit =
      new PauseTransition(Duration.millis(USER_EDIT_GRACE_MS));

  /**
   * Creates an empty card for a device. Call {@link #update} to fill it.
   *
   * @param deviceId the device's id
   */
  public DeviceCard(long deviceId) {
    this.deviceId = deviceId;
    getStyleClass().add("device-card");
    setSpacing(10);
    setPrefWidth(270);
    setMinWidth(270);
    setMaxWidth(270);

    title.getStyleClass().add("card-title");
    status.getStyleClass().add("status-pill");
    rooms.getStyleClass().add("muted");
    VBox header = new VBox(4, title, status);

    removeButton.getStyleClass().add("danger");
    removeButton.setMaxWidth(Double.MAX_VALUE);

    flipButton.setMaxWidth(Double.MAX_VALUE);
    flipButton.setVisible(false);
    flipButton.setManaged(false);

    restartButton.getStyleClass().add("primary");
    restartButton.setMaxWidth(Double.MAX_VALUE);

    getChildren().addAll(header, rooms, visualHolder, controls, flipButton, restartButton,
        removeButton);

    afterUserEdit.setOnFinished(event -> applyState(lastState));
  }

  /**
   * Returns the id of the device this card shows.
   *
   * @return the device's id
   */
  public long getDeviceId() {
    return deviceId;
  }

  /**
   * Sets who receives the commands given with the card's controls.
   *
   * @param listener the listener
   */
  public void setOnCommand(CommandListener listener) {
    this.commandListener = listener;
  }

  /**
   * Sets what happens when the user clicks Remove device.
   *
   * @param listener receives the device's id
   */
  public void setOnRemove(LongConsumer listener) {
    removeButton.setOnAction(event -> listener.accept(deviceId));
  }

  /**
   * Sets what happens when the owner clicks Restart device on an offline device.
   *
   * @param listener receives the device's id
   */
  public void setOnRestart(LongConsumer listener) {
    restartButton.setOnAction(event -> listener.accept(deviceId));
  }

  /**
   * Sets what happens when the user flips the switch. Only used for switches running in this
   * program; see {@link #setFlippable(boolean)}.
   *
   * @param listener receives the device's id
   */
  public void setOnFlip(LongConsumer listener) {
    flipButton.setOnAction(event -> listener.accept(deviceId));
  }

  /**
   * Shows or hides the Flip switch button. A switch has no commands; it changes when someone
   * flips it, which this program can only do for switches it created.
   *
   * @param flippable whether the device is a switch that can be flipped from here
   */
  public void setFlippable(boolean flippable) {
    flipButton.setVisible(flippable);
    flipButton.setManaged(flippable);
  }

  /**
   * Shows the device's current information.
   *
   * @param device the device
   * @param roomText the rooms the device is in, as text
   * @param canControl whether the user may send commands
   * @param canRemove whether the user may remove and restart the device
   */
  public void update(ControlledDevice device, String roomText, boolean canControl,
      boolean canRemove) {
    boolean online = device.isOnline();
    Map<String, String> state = device.getState();

    title.setText(device.getName().isBlank() ? "Device " + deviceId : device.getName());
    status.setText((online ? "online" : "offline") + " · #" + deviceId);
    status.getStyleClass().removeAll("online", "offline");
    status.getStyleClass().add(online ? "online" : "offline");
    rooms.setText(roomText);
    visualHolder.setOpacity(online ? 1.0 : 0.5);
    controls.setOpacity(online ? 1.0 : 0.5);

    DeviceVisual.Kind newKind = DeviceVisual.kindOf(device);
    if (newKind != kind) {
      kind = newKind;
      visual = DeviceVisual.create(newKind);
      visualHolder.getChildren().setAll(visual);
    }
    visual.update(state, online);

    Map<String, CommandSpec> commands = device.getCommands();
    String signature = newKind + " " + commands.values();
    if (!signature.equals(commandsShown)) {
      commandsShown = signature;
      buildControls(commands, newKind);
    }
    lastState = state;
    applyState(state);
    controls.setDisable(!canControl || !online);

    removeButton.setVisible(canRemove);
    removeButton.setManaged(canRemove);
    boolean canRestart = canRemove && !online;
    restartButton.setVisible(canRestart);
    restartButton.setManaged(canRestart);
  }

  // ---- Controls built from the announced commands ----

  private void buildControls(Map<String, CommandSpec> commands, DeviceVisual.Kind kind) {
    controls.getChildren().clear();
    controlUpdaters.clear();
    if (commands.isEmpty()) {
      Label none = new Label(switch (kind) {
        case SWITCH -> "Switch: reports on or off, takes no commands.";
        case SENSOR -> "Sensor: reports readings, takes no commands.";
        default -> "No commands yet: the device has not announced any, or is offline.";
      });
      none.getStyleClass().add("muted");
      none.setWrapText(true);
      controls.getChildren().add(none);
      return;
    }
    FlowPane actionButtons = new FlowPane(6, 6);
    for (CommandSpec spec : commands.values()) {
      switch (spec.getType()) {
        case BOOLEAN -> controls.getChildren().add(switchControl(spec));
        case INTEGER, DECIMAL -> controls.getChildren().add(sliderControl(spec));
        case ENUM -> controls.getChildren().add(optionsControl(spec));
        case NONE -> actionButtons.getChildren().add(actionButton(spec));
        default -> { }
      }
    }
    if (!actionButtons.getChildren().isEmpty()) {
      controls.getChildren().add(actionButtons);
    }
  }

  private HBox row(String name, Node control) {
    Label label = new Label(Texts.pretty(name));
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox row = new HBox(8, label, spacer, control);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private HBox switchControl(CommandSpec spec) {
    ToggleButton toggle = new ToggleButton("OFF");
    toggle.getStyleClass().add("switch");
    toggle.setOnAction(event -> {
      boolean wanted = toggle.isSelected();
      toggle.setText(wanted ? "ON" : "OFF");
      send(spec.getName(), String.valueOf(wanted));
    });
    controlUpdaters.add(state -> {
      String key = stateKeyFor(spec.getName(), state);
      if (key != null && !recentlyEdited()) {
        boolean on = Boolean.parseBoolean(state.get(key));
        toggle.setSelected(on);
        toggle.setText(on ? "ON" : "OFF");
      }
    });
    return row(spec.getName(), toggle);
  }

  private VBox sliderControl(CommandSpec spec) {
    boolean whole = spec.getType() == ParameterType.INTEGER;
    Slider slider = new Slider(spec.getMin(), spec.getMax(), spec.getMin());
    slider.setBlockIncrement(whole ? 1 : 0.5);
    Label value = new Label();
    value.getStyleClass().add("slider-value");
    slider.valueProperty().addListener((obs, old, now) ->
        value.setText(format(now.doubleValue(), whole)));
    value.setText(format(slider.getValue(), whole));
    Runnable sendValue = () -> send(spec.getName(), format(slider.getValue(), whole));
    slider.setOnMouseReleased(event -> sendValue.run());
    slider.setOnKeyReleased(event -> sendValue.run());
    controlUpdaters.add(state -> {
      String key = stateKeyFor(spec.getName(), state);
      if (key != null && !slider.isPressed() && !slider.isValueChanging() && !recentlyEdited()) {
        try {
          slider.setValue(Double.parseDouble(state.get(key)));
        } catch (NumberFormatException ignored) {
          // Not a number; leave the slider where it is.
        }
      }
    });
    return new VBox(2, row(spec.getName(), value), slider);
  }

  private VBox optionsControl(CommandSpec spec) {
    ToggleGroup group = new ToggleGroup();
    HBox buttons = new HBox(4);
    for (String option : spec.getOptions()) {
      ToggleButton button = new ToggleButton(option);
      button.setToggleGroup(group);
      button.getStyleClass().add("option");
      button.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(button, Priority.ALWAYS);
      button.setOnAction(event -> {
        button.setSelected(true);
        send(spec.getName(), option);
      });
      buttons.getChildren().add(button);
    }
    controlUpdaters.add(state -> {
      String key = stateKeyFor(spec.getName(), state);
      if (key != null && !recentlyEdited()) {
        for (var toggle : group.getToggles()) {
          ToggleButton button = (ToggleButton) toggle;
          button.setSelected(button.getText().equals(state.get(key)));
        }
      }
    });
    return new VBox(4, new Label(Texts.pretty(spec.getName())), buttons);
  }

  private Button actionButton(CommandSpec spec) {
    Button button = new Button(Texts.pretty(spec.getName()));
    button.getStyleClass().add("action");
    button.setOnAction(event -> send(spec.getName(), ""));
    return button;
  }

  /** Sets every control to the given state, except while the user is using them. */
  private void applyState(Map<String, String> state) {
    for (Consumer<Map<String, String>> updater : controlUpdaters) {
      updater.accept(state);
    }
  }

  /**
   * Reports a command to the listener.
   *
   * <p>For a short while afterwards the controls are not changed by incoming state, so they do
   * not jump back before the device has answered. A command can change more than the control
   * that sent it, for example a light turns on when given a brightness, and that change may
   * arrive during this time. So when the time is up, the latest state is shown.
   */
  private void send(String command, String argument) {
    lastUserEdit = System.currentTimeMillis();
    afterUserEdit.playFromStart();
    if (commandListener != null) {
      commandListener.onCommand(deviceId, command, argument);
    }
  }

  /** Whether the user used a control so recently that the device may not have answered yet. */
  private boolean recentlyEdited() {
    return System.currentTimeMillis() - lastUserEdit < USER_EDIT_GRACE_MS;
  }

  /**
   * Finds the state value a command changes: the value with the same name, such as
   * {@code speed}, or else one that starts with the name, such as {@code powerOn} for
   * {@code power}.
   */
  private static String stateKeyFor(String command, Map<String, String> state) {
    if (state.containsKey(command)) {
      return command;
    }
    for (String key : state.keySet()) {
      if (key.startsWith(command)) {
        return key;
      }
    }
    return null;
  }

  private static String format(double value, boolean whole) {
    return whole
        ? String.valueOf(Math.round(value))
        : String.format(Locale.ROOT, "%.1f", Math.round(value * 10) / 10.0);
  }
}
