package edu.ntnu.bidata.group1.device.actuator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Heater}, verifying its initial state, registered
 * commands, supported target temperatures, and the effect of command execution
 * on the heater's reported state.
 */
class HeaterTest {

  /**
   * Test subclass exposing the protected
   * {@link Heater#executeCommand(String, String)} method.
   */
  private static class TestHeater extends Heater {

    TestHeater() {
      super("test-heater");
    }

    void execute(String command, String argument) {
      executeCommand(command, argument);
    }
  }

  /**
   * Verifies that a newly created {@link Heater} starts powered off and with the
   * documented default target temperature.
   *
   * <p>The constructor initializes the {@code powerOn} property to
   * {@code false} and the {@code targetTemperature} property to {@code 21.0}.
   * These values are expected to be reported before any commands are executed.
   */
  @Test
  void newHeaterStartsPoweredOffAtDefaultTemperature() {
    Heater heater = new Heater("living-room-heater");

    assertFalse(heater.isPowerOn());
    assertEquals(21.0, heater.getTargetTemperature());
  }

  /**
   * Verifies that the heater registers exactly the commands documented by the
   * class.
   *
   * <p>A heater is expected to support the {@code power} command and the
   * {@code target} command. No additional commands should be present.
   */
  @Test
  void availableCommandsContainPowerAndTarget() {
    Heater heater = new Heater("living-room-heater");

    List<CommandSpec> commands = heater.getAvailableCommands();

    assertEquals(2, commands.size());

    assertEquals(Heater.POWER, commands.get(0).getName());
    assertEquals(Heater.TARGET, commands.get(1).getName());
  }

  /**
   * Verifies that executing the {@value Heater#POWER} command with the value
   * {@code true} turns the heater on.
   *
   * <p>After execution, {@link Heater#isPowerOn()} is expected to return
   * {@code true}.
   */
  @Test
  void powerTrueTurnsHeaterOn() {
    TestHeater heater = new TestHeater();

    heater.execute(Heater.POWER, "true");

    assertTrue(heater.isPowerOn());
  }

  /**
   * Verifies that executing the {@value Heater#POWER} command with the value
   * {@code false} turns the heater off.
   *
   * <p>The test first powers the heater on and then off again.
   */
  @Test
  void powerFalseTurnsHeaterOff() {
    TestHeater heater = new TestHeater();

    heater.execute(Heater.POWER, "true");
    assertTrue(heater.isPowerOn());

    heater.execute(Heater.POWER, "false");

    assertFalse(heater.isPowerOn());
  }

  /**
   * Verifies that changing the target temperature updates the reported target
   * temperature.
   *
   * <p>The target temperature is changed from the default value to
   * {@code 25.5 °C}. The heater is expected to report the new target value.
   */
  @Test
  void targetCommandUpdatesTargetTemperature() {
    TestHeater heater = new TestHeater();

    heater.execute(Heater.TARGET, "25.5");

    assertEquals(25.5, heater.getTargetTemperature());
  }

  /**
   * Verifies that successive command executions leave the heater in the state
   * represented by the most recently executed commands.
   *
   * <p>The heater is powered on, assigned a new target temperature and then
   * powered off again. The target temperature should remain unchanged while the
   * power state reflects the last power command executed.
   */
  @Test
  void commandsUpdateStateCorrectly() {
    TestHeater heater = new TestHeater();

    heater.execute(Heater.POWER, "true");
    heater.execute(Heater.TARGET, "24.0");

    assertTrue(heater.isPowerOn());
    assertEquals(24.0, heater.getTargetTemperature());

    heater.execute(Heater.POWER, "false");

    assertFalse(heater.isPowerOn());
    assertEquals(24.0, heater.getTargetTemperature());
  }

  /**
   * Verifies that the documented command constants contain the expected protocol
   * values.
   *
   * <p>The constants are used in command specifications and protocol messages
   * and therefore must not change unintentionally.
   */
  @Test
  void commandConstantsHaveExpectedValues() {
    assertEquals("power", Heater.POWER);
    assertEquals("target", Heater.TARGET);
  }
}