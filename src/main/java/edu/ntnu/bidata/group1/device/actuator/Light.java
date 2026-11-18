package edu.ntnu.bidata.group1.device.actuator;

/**
 * Actuator that controls a light by its power state and brightness value.
 *
 * <p>The light accepts two commands, which it announces to controllers:
 * <ul>
 *   <li>{@code power} - BOOLEAN, turns the light on or off.</li>
 *   <li>{@code value} - INTEGER from 0 to 100, the brightness in percent. Setting a value
 *       above 0 also turns the light on.</li>
 * </ul>
 * Arguments outside these ranges are rejected by {@link Actuator} before they reach this class.
 */
public class Light extends Actuator {

  // Valid commands.
  public static final String POWER = "power";
  public static final String VALUE = "value";


  /**
   * Creates a light with a random name, such as {@code light-3f9a1c2e}. See
   * {@link #Light(String)}.
   */
  public Light() {
    this(randomName("light"));
  }

  /**
   * Creates a light actuator that is initially powered off. Call {@link #start()} to connect it.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code kitchen-light}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public Light(String name) {
    super(name);
    properties.put("value", 0L);
    properties.put("powerOn", false);
    registerCommand(CommandSpec.bool(POWER));
    registerCommand(CommandSpec.integer(VALUE, 0, 100));
  }

  @Override
  protected void executeCommand(String name, String argument) {
    switch (name) {
      case POWER -> properties.put("powerOn", Boolean.parseBoolean(argument));
      case VALUE -> properties.put("value", Long.parseLong(argument));
      default -> { }
    }
  }

  /**
   * Returns the current brightness.
   *
   * @return the brightness in percent, from 0 to 100
   */
  public long getValue() {
    return (long) this.properties.get("value");
  }

  /**
   * Returns whether the light is on.
   *
   * @return {@code true} if the light is on
   */
  public boolean isPowerOn() {
    return (boolean) this.properties.get("powerOn");
  }
}
