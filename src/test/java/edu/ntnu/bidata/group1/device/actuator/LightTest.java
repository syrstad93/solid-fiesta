package edu.ntnu.bidata.group1.device.actuator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Light}, verifying its initial state, registered
 * commands, brightness handling, and the effect of command execution on the
 * reported power state and brightness value.
 */
class LightTest {

  /**
   * Test subclass exposing the protected
   * {@link Light#executeCommand(String, String)} method.
   */
  private static class TestLight extends Light {

    TestLight() {
      super("test-light");
    }

    void execute(String command, String argument) {
      executeCommand(command, argument);
    }
  }

  /**
   * Verifies that a newly created {@link Light} starts powered off with a
   * brightness value of {@code 0}.
   *
   * <p>The constructor initializes the {@code value} property to {@code 0}
   * and the {@code powerOn} property to {@code false}. These values are
   * expected to be reported immediately after construction.
   */
  @Test
  void newLightStartsPoweredOffWithZeroBrightness() {
    Light light = new Light("kitchen-light");

    assertEquals(0, light.getValue());
    assertFalse(light.isPowerOn());
  }

  /**
   * Verifies that the light registers exactly the commands documented by the
   * class.
   *
   * <p>A light is expected to support the {@code power} command and the
   * {@code value} command. No additional commands should be registered.
   */
  @Test
  void availableCommandsContainPowerAndValue() {
    Light light = new Light("kitchen-light");

    List<CommandSpec> commands = light.getAvailableCommands();

    assertEquals(2, commands.size());

    assertEquals(Light.POWER, commands.get(0).getName());
    assertEquals(Light.VALUE, commands.get(1).getName());
  }

  /**
   * Verifies that executing the {@value Light#POWER} command with the value
   * {@code true} turns the light on.
   *
   * <p>After execution, {@link Light#isPowerOn()} is expected to return
   * {@code true}.
   */
  @Test
  void powerTrueTurnsLightOn() {
    TestLight light = new TestLight();

    light.execute(Light.POWER, "true");

    assertTrue(light.isPowerOn());
  }

  /**
   * Verifies that executing the {@value Light#POWER} command with the value
   * {@code false} turns the light off.
   *
   * <p>The light is first turned on and then off again. The power state is
   * expected to reflect the most recent command.
   */
  @Test
  void powerFalseTurnsLightOff() {
    TestLight light = new TestLight();

    light.execute(Light.POWER, "true");
    assertTrue(light.isPowerOn());

    light.execute(Light.POWER, "false");

    assertFalse(light.isPowerOn());
  }

  /**
   * Verifies that setting a brightness value updates the reported brightness.
   *
   * <p>The value {@code 50} is assigned through the {@code value} command.
   * The light is expected to report the same value through
   * {@link Light#getValue()}.
   */
  @Test
  void valueCommandUpdatesBrightness() {
    TestLight light = new TestLight();

    light.execute(Light.VALUE, "50");

    assertEquals(50, light.getValue());
  }

  /**
   * Verifies that assigning a brightness value greater than zero automatically
   * powers the light on.
   *
   * <p>According to the documented behaviour, a light that receives a positive
   * brightness value should be considered on.
   */
  @Test
  void positiveBrightnessTurnsLightOn() {
    TestLight light = new TestLight();

    light.execute(Light.VALUE, "75");

    assertEquals(75, light.getValue());
    assertTrue(light.isPowerOn());
  }

  /**
   * Verifies that assigning a brightness value of {@code 0} does not
   * automatically power the light on.
   *
   * <p>The value is updated, but the documented implementation only turns the
   * light on when the assigned value is greater than zero.
   */
  @Test
  void zeroBrightnessDoesNotTurnLightOn() {
    TestLight light = new TestLight();

    light.execute(Light.VALUE, "0");

    assertEquals(0, light.getValue());
    assertFalse(light.isPowerOn());
  }

  /**
   * Verifies that successive command executions leave the light in the state
   * represented by the most recently executed commands.
   *
   * <p>The light is assigned a brightness value, powered off, and assigned a new
   * brightness value. The final state is expected to reflect the latest
   * brightness setting and to be powered on.
   */
  @Test
  void commandsUpdateStateCorrectly() {
    TestLight light = new TestLight();

    light.execute(Light.VALUE, "25");

    assertEquals(25, light.getValue());
    assertTrue(light.isPowerOn());

    light.execute(Light.POWER, "false");

    assertFalse(light.isPowerOn());

    light.execute(Light.VALUE, "80");

    assertEquals(80, light.getValue());
    assertTrue(light.isPowerOn());
  }

  /**
   * Verifies that the documented command constants contain the expected
   * protocol values.
   *
   * <p>The constants are used in command specifications and protocol messages
   * and therefore must not change unintentionally.
   */
  @Test
  void commandConstantsHaveExpectedValues() {
    assertEquals("power", Light.POWER);
    assertEquals("value", Light.VALUE);
  }
}