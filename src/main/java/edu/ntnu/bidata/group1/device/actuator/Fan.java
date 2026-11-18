package edu.ntnu.bidata.group1.device.actuator;

/**
 * Actuator that controls a fan by its power state and speed.
 *
 * <p>The fan accepts two commands, which it announces to controllers:
 * <ul>
 *   <li>{@code power} - BOOLEAN, turns the fan on or off.</li>
 *   <li>{@code speed} - ENUM, one of {@code low}, {@code medium} or {@code high}. Setting
 *       the speed also turns the fan on.</li>
 * </ul>
 * It reports the properties {@code type}, {@code powerOn} and {@code speed}. The fan starts off,
 * at speed {@code low}.
 */
public class Fan extends Actuator {

  // Valid commands.
  public static final String POWER = "power";
  public static final String SPEED = "speed";

  /**
   * Creates a fan with a random name, such as {@code fan-3f9a1c2e}. See
   * {@link #Fan(String)}.
   */
  public Fan() {
    this(randomName("fan"));
  }

  /**
   * Creates a fan that is initially off, at speed {@code low}. Call {@link #start()} to connect it.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code kitchen-fan}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public Fan(String name) {
    super(name);
    properties.put("powerOn", false);
    properties.put("speed", "low");
    registerCommand(CommandSpec.bool(POWER));
    registerCommand(CommandSpec.options(SPEED, "low", "medium", "high"));
  }

  @Override
  protected void executeCommand(String name, String argument) {
    switch (name) {
      case POWER -> properties.put("powerOn", Boolean.parseBoolean(argument));
      case SPEED -> {
        properties.put("speed", argument);
        properties.put("powerOn", true);
      }
      default -> {}
    }
  }

  /**
   * Returns whether the fan is on.
   *
   * @return {@code true} if the fan is on
   */
  public boolean isPowerOn() {
    return (boolean) properties.get("powerOn");
  }

  /**
   * Returns the fan's speed setting.
   *
   * @return {@code low}, {@code medium} or {@code high}
   */
  public String getSpeed() {
    return (String) properties.get("speed");
  }
}
