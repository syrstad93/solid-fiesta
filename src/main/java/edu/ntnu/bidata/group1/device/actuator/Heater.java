package edu.ntnu.bidata.group1.device.actuator;

/**
 * Actuator that controls a heater by its power state and target temperature.
 *
 * <p>The heater accepts two commands, which it announces to controllers:
 * <ul>
 *   <li>{@code power} - BOOLEAN, turns the heater on or off.</li>
 *   <li>{@code target} - DECIMAL from 5.0 to 30.0, the temperature in degrees Celsius the
 *       heater aims for. Changing the target does not turn the heater on or off.</li>
 * </ul>
 * It reports the properties {@code type}, {@code powerOn} and {@code targetTemperature}. The
 * heater starts off, with a target of 21.0 °C.
 *
 * <p>The heater is simulated: it does not change what a {@code TemperatureSensor} measures.
 */
public class Heater extends Actuator {

  // Valid commands.
  public static final String POWER = "power";
  public static final String TARGET = "target";

  /**
   * Creates a heater with a random name, such as {@code heater-3f9a1c2e}. See
   * {@link #Heater(String)}.
   */
  public Heater() {
    this(randomName("heater"));
  }

  /**
   * Creates a heater that is initially off, with a target of 21.0 °C. Call {@link #start()} to
   * connect it.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code kitchen-heater}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public Heater(String name) {
    super(name);
    properties.put("powerOn", false);
    properties.put("targetTemperature", 21.0);
    registerCommand(CommandSpec.bool(POWER));
    registerCommand(CommandSpec.decimal(TARGET, 5.0, 30.0));
  }

  @Override
  protected void executeCommand(String name, String argument) {
    switch (name) {
      case POWER -> properties.put("powerOn", Boolean.parseBoolean(argument));
      case TARGET -> {
        properties.put("targetTemperature", Double.parseDouble(argument));
        properties.put("powerOn", true);
      }
      default -> {}
    }
  }

  /**
   * Returns whether the heater is on.
   *
   * @return {@code true} if the heater is on
   */
  public boolean isPowerOn() {
    return (boolean) properties.get("powerOn");
  }

  /**
   * Returns the temperature the heater aims for.
   *
   * @return the target in degrees Celsius
   */
  public double getTargetTemperature() {
    return (double) properties.get("targetTemperature");
  }
}
