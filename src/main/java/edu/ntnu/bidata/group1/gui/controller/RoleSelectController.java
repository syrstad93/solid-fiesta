package edu.ntnu.bidata.group1.gui.controller;

import java.io.IOException;
import java.util.function.Consumer;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.gui.demo.DemoHome;
import edu.ntnu.bidata.group1.gui.view.RoleSelectView;
import edu.ntnu.bidata.group1.utility.Role;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import javafx.application.Platform;

/**
 * The GUI controller for the role selection window.
 *
 * <p>When the user clicks Connect, it creates a device {@link Controller} with the chosen
 * {@link Role} and name, and starts it. Connecting can take a few seconds, so it is done on a
 * background thread. Once connected, the controller is handed on, and the main window opens.
 */
public class RoleSelectController {

  private final RoleSelectView view;
  private final Consumer<Controller> onConnected;

  /**
   * Connects the view's buttons.
   *
   * @param view the view
   * @param onConnected receives each controller once it is connected, on the JavaFX thread
   */
  public RoleSelectController(RoleSelectView view, Consumer<Controller> onConnected) {
    this.view = view;
    this.onConnected = onConnected;
    view.setOnConnect(this::connect);
    view.setOnStartDemo(this::startDemo);
  }

  /** Creates and starts a controller with the chosen role and name. */
  public void connect() {
    Role role = view.getSelectedRole();
    String name = view.getControllerName().trim();
    if (!name.isEmpty()) {
      try {
        Device.checkName(name);
      } catch (IllegalArgumentException e) {
        view.showStatus(e.getMessage(), true);
        return;
      }
    }
    Controller controller = name.isEmpty() ? new Controller(role) : new Controller(name, role);
    view.setConnecting(true);
    view.showStatus("Connecting " + controller.getName() + " as " + role + "…", false);

    Thread connector = new Thread(() -> {
      try {
        controller.start();
        Platform.runLater(() -> {
          view.setConnecting(false);
          view.clearName();
          view.showStatus("Connected " + controller.getName() + " as " + role
              + ". You can connect another controller with a different role to compare.",
              false);
          onConnected.accept(controller);
        });
      } catch (IOException e) {
        Platform.runLater(() -> {
          view.setConnecting(false);
          view.showStatus("Could not reach the server at "
              + UniversallyAvailableInformation.LOCALHOST + ":"
              + UniversallyAvailableInformation.PORT_NUMBER
              + ". Start the server (Main), or click Start demo home below.", true);
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "connect-" + controller.getName());
    connector.setDaemon(true);
    connector.start();
  }

  /** Starts the demo home: a server with simulated devices, in this program. */
  public void startDemo() {
    view.showDemoStatus("Starting the server and devices…", false);
    Thread starter = new Thread(() -> {
      try {
        String description = DemoHome.start();
        Platform.runLater(() -> view.showDemoStatus(description, false));
      } catch (IOException e) {
        Platform.runLater(() -> view.showDemoStatus("Could not start the demo server: "
            + e.getMessage() + ". Is a server already running? Then just connect.", true));
      } catch (IllegalStateException e) {
        Platform.runLater(() -> view.showDemoStatus(e.getMessage(), false));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (RuntimeException e) {
        Platform.runLater(() -> view.showDemoStatus("The demo home could not be set up: "
            + e.getMessage(), false));
      }
    }, "demo-home");
    starter.setDaemon(true);
    starter.start();
  }
}
