package edu.ntnu.bidata.group1.device.sensor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link Switch} class.
 *
 * <p>
 * AI has assisted in writing the tests,
 * but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class SwitchTest {

  /**
   * Tests that a new switch is initially off.
   */
  @Test
  public void switchStartsOff() {
    Switch deviceSwitch = new Switch();

    assertFalse(deviceSwitch.isOn());
  }

  /**
   * Tests that turnOn sets the switch to on.
   */
  @Test
  public void turnOnSetsSwitchOn() {
    Switch deviceSwitch = new Switch();

    deviceSwitch.turnOn();

    assertTrue(deviceSwitch.isOn());
  }

  /**
   * Tests that turnOff sets the switch to off.
   */
  @Test
  public void turnOffSetsSwitchOff() {
    Switch deviceSwitch = new Switch();

    deviceSwitch.turnOn();
    deviceSwitch.turnOff();

    assertFalse(deviceSwitch.isOn());
  }

  /**
   * Tests that toggle turns the switch on when it is off.
   */
  @Test
  public void toggleTurnsOnWhenOff() {
    Switch deviceSwitch = new Switch();

    deviceSwitch.toggle();

    assertTrue(deviceSwitch.isOn());
  }

  /**
   * Tests that toggle turns the switch off when it is on.
   */
  @Test
  public void toggleTurnsOffWhenOn() {
    Switch deviceSwitch = new Switch();

    deviceSwitch.turnOn();
    deviceSwitch.toggle();

    assertFalse(deviceSwitch.isOn());
  }

  /**
   * Tests that multiple toggles alternate the state.
   */
  @Test
  public void toggleAlternatesState() {
    Switch deviceSwitch = new Switch();

    deviceSwitch.toggle();
    assertTrue(deviceSwitch.isOn());

    deviceSwitch.toggle();
    assertFalse(deviceSwitch.isOn());

    deviceSwitch.toggle();
    assertTrue(deviceSwitch.isOn());
  }

  /**
   * Tests that the named constructor creates a switch in the off state.
   */
  @Test
  public void namedConstructorStartsOff() {
    Switch deviceSwitch = new Switch("Kitchen Switch");

    assertFalse(deviceSwitch.isOn());
  }
}