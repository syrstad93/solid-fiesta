package edu.ntnu.bidata.group1.gui;

import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.gui.controller.DashboardController;
import edu.ntnu.bidata.group1.gui.controller.RoleSelectController;
import edu.ntnu.bidata.group1.gui.model.HomeModel;
import edu.ntnu.bidata.group1.gui.view.DashboardView;
import edu.ntnu.bidata.group1.gui.view.RoleSelectView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The smart home GUI.
 *
 * <p>Built with the model-view-controller design:
 * <ul>
 *   <li><b>Model</b>: the device {@link Controller}, which holds the devices, rooms and
 *       automations, wrapped in a {@link HomeModel} that tells the views when they change.</li>
 *   <li><b>Views</b>: {@link RoleSelectView} and {@link DashboardView}, with its tabs. They
 *       only show data and report what the user does.</li>
 *   <li><b>Controllers</b>: {@link RoleSelectController} and {@link DashboardController}.
 *       They turn what the user does into calls on the device {@link Controller}.</li>
 * </ul>
 *
 * <p>The first window lets the user pick a role and connect. Each connection opens its own
 * window, so an owner, a user and a guest can be compared side by side. Start with
 * {@code --demo} to start a demo home with simulated devices at once.
 */
public class SmartHomeApp extends Application {

  private RoleSelectController roleSelectController;

  @Override
  public void start(Stage stage) {
    RoleSelectView view = new RoleSelectView();
    roleSelectController = new RoleSelectController(view, this::openDashboard);

    Scene scene = new Scene(view, 620, 640);
    style(scene);
    stage.setTitle("Smart Home – choose role");
    stage.setScene(scene);
    stage.show();

    if (getParameters().getRaw().contains("--demo")) {
      roleSelectController.startDemo();
    }
  }

  /**
   * Opens the main window for a connected controller.
   *
   * @param controller the connected controller
   */
  private void openDashboard(Controller controller) {
    HomeModel model = new HomeModel(controller);
    DashboardView view = new DashboardView();
    DashboardController dashboard = new DashboardController(model, view);

    Stage stage = new Stage();
    Scene scene = new Scene(view, 1120, 780);
    style(scene);
    stage.setTitle("Smart Home – " + controller.getName() + " (" + controller.getRole() + ")");
    stage.setScene(scene);
    stage.setOnHidden(event -> dashboard.stop());
    stage.show();
    dashboard.start();
  }

  private static void style(Scene scene) {
    var css = SmartHomeApp.class.getResource("style.css");
    if (css != null) {
      scene.getStylesheets().add(css.toExternalForm());
    }
  }

  /**
   * Ends the program when the last window is closed. The network connections run on their own
   * threads, which would otherwise keep the program running.
   */
  @Override
  public void stop() {
    System.exit(0);
  }
}
