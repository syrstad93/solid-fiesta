package edu.ntnu.bidata.group1.gui.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.device.actuator.DoorLock;
import edu.ntnu.bidata.group1.device.actuator.Fan;
import edu.ntnu.bidata.group1.device.actuator.Heater;
import edu.ntnu.bidata.group1.device.actuator.Light;
import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.device.sensor.TemperatureSensor;
import edu.ntnu.bidata.group1.network.server.Server;
import edu.ntnu.bidata.group1.utility.Condition;
import edu.ntnu.bidata.group1.utility.Role;

/**
 * Starts a small smart home in this program, so the GUI has something to show: the server, some
 * simulated devices, rooms and automations.
 *
 * <p>The home is set up through an ordinary {@link Controller} with the {@link Role#OWNER} role,
 * called {@code demo-setup}, the same way a person would do it in the GUI. One light, the
 * {@code hallway-light}, is connected but left unregistered, so an owner can try adding it.
 *
 * <p>This is only for trying out the GUI. In normal use the server is started with
 * {@link edu.ntnu.bidata.group1.Main} and the devices run on their own.
 */
public final class DemoHome {

  private static final long WAIT_MS = 5000;
  private static boolean started;

  private DemoHome() {
  }

  /**
   * Starts the server, the simulated devices, and sets up rooms and automations. Blocks until
   * everything is ready, so call it from a background thread.
   *
   * @return a short text describing the home, for showing to the user
   * @throws IOException if the server cannot be started, for example because another server is
   *     already running on the same port
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalStateException if the demo home has already been started
   */
  public static synchronized String start() throws IOException, InterruptedException {
    if (started) {
      throw new IllegalStateException("The demo home is already running.");
    }
    Server server = Server.getInstance();
    CountDownLatch ready = new CountDownLatch(1);
    Thread serverThread = new Thread(() -> {
      try {
        server.start(ready);
      } catch (IOException e) {
        System.out.println("Demo server stopped: " + e.getMessage());
      }
    }, "demo-server");
    serverThread.setDaemon(true);
    serverThread.start();
    if (!ready.await(WAIT_MS, TimeUnit.MILLISECONDS)) {
      throw new IOException("The demo server did not start in time.");
    }
    started = true;

    long livingLight = connect(server, new Light("living-room-light"));
    long heater = connect(server, new Heater("living-room-heater"));
    long thermometer = connect(server, new TemperatureSensor("living-room-thermometer"));
    long fan = connect(server, new Fan("bedroom-fan"));
    long frontDoor = connect(server, new DoorLock("front-door"));
    long hallwayLight = connect(server, new Light("hallway-light"));

    Controller setup = new Controller("demo-setup", Role.OWNER);
    setup.start();
    for (long id : List.of(livingLight, heater, thermometer, fan, frontDoor)) {
      setup.addDevice(id).join();
    }

    long livingRoom = setup.createRoom("Living room").join();
    long bedroom = setup.createRoom("Bedroom").join();
    long entrance = setup.createRoom("Entrance").join();
    setup.addDeviceToRoom(livingRoom, livingLight).join();
    setup.addDeviceToRoom(livingRoom, heater).join();
    setup.addDeviceToRoom(livingRoom, thermometer).join();
    setup.addDeviceToRoom(bedroom, fan).join();
    setup.addDeviceToRoom(entrance, frontDoor).join();

    // addAutomation checks commands and readings, which arrive from the devices a moment later.
    waitFor(() -> setup.getDevice(heater).flatMap(d -> d.getCommand(Heater.POWER)).isPresent()
        && setup.getDevice(fan).flatMap(d -> d.getCommand(Fan.SPEED)).isPresent()
        && setup.getDevice(thermometer)
            .map(d -> d.getState().containsKey(TemperatureSensor.READING)).orElse(false));
    List<String> problems = new ArrayList<>();
    addAutomation(setup, problems, thermometer, Condition.BELOW, 20.5, heater,
        Heater.POWER, "true");
    addAutomation(setup, problems, thermometer, Condition.ABOVE, 22.5, heater,
        Heater.POWER, "false");
    addAutomation(setup, problems, thermometer, Condition.ABOVE, 22.5, fan,
        Fan.SPEED, "high");

    String text = "Demo home running: 5 registered devices in 3 rooms, "
        + (3 - problems.size()) + " automations.\n"
        + "Not registered yet: hallway-light with id " + hallwayLight
        + ". Connect as OWNER to add it.";
    if (!problems.isEmpty()) {
      text += "\nSome automations could not be added: " + String.join("; ", problems);
    }
    return text;
  }

  /**
   * Starts a device and returns the id the server gave it. Ids are handed out in order as
   * clients connect, so the device gets the next free id.
   */
  private static long connect(Server server, Device device)
      throws IOException, InterruptedException {
    long id = server.peekNextAvailableId();
    device.start();
    waitFor(() -> server.peekNextAvailableId() > id);
    return id;
  }

  private static void addAutomation(Controller setup, List<String> problems, long sensor,
      Condition condition, double threshold, long device, String command, String argument) {
    try {
      setup.addAutomation(sensor, TemperatureSensor.READING, condition, threshold, device,
          command, argument).join();
    } catch (RuntimeException e) {
      problems.add(e.getMessage());
    }
  }

  private static void waitFor(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + WAIT_MS;
    while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
  }
}
