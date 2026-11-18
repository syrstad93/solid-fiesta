package edu.ntnu.bidata.group1.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ntnu.bidata.group1.device.actuator.Light;
import edu.ntnu.bidata.group1.device.controller.Controller;
import edu.ntnu.bidata.group1.device.sensor.TemperatureSensor;
import edu.ntnu.bidata.group1.utility.Role;
import org.junit.jupiter.api.Test;

/**
 * Tests the lifecycle of simulated smart home devices and controllers.
 *
 * The tests check that creating a device only initializes its name, default properties, and
 * commands, and that connecting to the server requires an explicit call to {@code start()}. They
 * also check that a {@link TemperatureSensor} keeps readings it receives before it is started,
 * that a {@link Controller} rejects device and state requests until it is started, and that a
 * {@link Controller} cannot be created without a role.
 */
class DeviceLifecycleTest {

  /**
   * Verifies that creating a {@link Light} does not connect it to the server.
   *
   * <p>A newly constructed light must report that it has not been started, and its
   * brightness must be {@code 0}. Both checks confirm that the constructor only sets up
   * the device's name, default properties, and commands, and that connecting requires an
   * explicit call to {@code start()}.
   */
  @Test
  void creatingADeviceDoesNotConnectIt() {
    Light light = new Light("lifecycle-light");
    assertFalse(light.isStarted());
    assertEquals(0L, light.getValue());
  }

  /**
   * Verifies that a {@link TemperatureSensor} keeps the readings it receives even when it has not
   * been started.
   *
   * <p>The test sets the simulated temperature to 30 °C while the sensor is still disconnected,
   * checks that {@link TemperatureSensor#isStarted()} returns {@code false}, and then checks that
   * {@link TemperatureSensor#getReading(String)} returns the stored value of 30.0 for
   * {@link TemperatureSensor#READING}.
   */
  @Test
  void aSensorStoresReadingsBeforeItIsStarted() {
    TemperatureSensor sensor = new TemperatureSensor("lifecycle-temperature");
    sensor.setTemperature(30);
    assertFalse(sensor.isStarted());
    assertEquals(30.0, sensor.getReading(TemperatureSensor.READING).orElseThrow().doubleValue());
  }

  /**
   * Verifies that a {@link Controller} rejects device and state requests while it is not yet started.
   *
   * <p>This test creates a controller with the {@code OWNER} role and checks that calling
   * {@code addDevice} and {@code requestHomeState} before the controller has been started
   * throws an {@link IllegalStateException}.
   */
  @Test
  void aControllerCannotSendRequestsBeforeItIsStarted() {
    Controller owner = new Controller("lifecycle-owner", Role.OWNER);
    assertThrows(IllegalStateException.class, () -> owner.addDevice(1));
    assertThrows(IllegalStateException.class, owner::requestHomeState);
  }

  /**
   * Verifies that a {@code Controller} cannot be created without a role.
   * Passing {@code null} as the role must cause the constructor to throw an
   * {@link IllegalArgumentException}.
   */
  @Test
  void aControllerNeedsARole() {
    assertThrows(IllegalArgumentException.class, () -> new Controller("no-role", null));
  }
}
