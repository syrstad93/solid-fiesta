package edu.ntnu.bidata.group1.gui;

import javafx.application.Application;

/**
 * Starts the GUI. A separate class from {@link SmartHomeApp}, so the program can be run from
 * an IDE or a jar without JavaFX being set up as modules.
 *
 * <p>Run with {@code --demo} to also start a server with simulated devices in this program.
 */
public class Launcher {

  private Launcher() {
  }

  /**
   * Starts the GUI.
   *
   * @param args {@code --demo} to start a demo home at once
   */
  public static void main(String[] args) {
    Application.launch(SmartHomeApp.class, args);
  }
}
