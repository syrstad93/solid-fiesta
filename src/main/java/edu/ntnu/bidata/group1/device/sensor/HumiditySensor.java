package edu.ntnu.bidata.group1.device.sensor;

/**
 * Sensor that measures relative air humidity in percent.
 *
 * <p>Reports the reading {@value #READING}. The simulated humidity starts at 45 % and drifts by
 * up to 1 percentage point per measurement, between 0 and 100 %.
 */
public class HumiditySensor extends Sensor {

  /** Name of the reading this sensor reports. */
  public static final String READING = "humidity";

  private final SimulatedValue humidity = new SimulatedValue(45.0, 0.0, 100.0, 1.0);

  /**
   * Creates a humidity sensor with a random name, such as {@code humidity-sensor-3f9a1c2e}. See
   * {@link #HumiditySensor(String)}.
   */
  public HumiditySensor() {
    this(randomName("humidity-sensor"));
  }

  /**
   * Creates a humidity sensor. Call {@link #start()} to connect it and start
   * measuring.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code living-room-humidity}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public HumiditySensor(String name) {
    super(name);
  }

  @Override
  protected void measure() {
    report(READING, humidity.next());
  }

  /**
   * Sets the simulated humidity and reports it right away. Useful for demos and tests, for
   * example to trigger an automation.
   *
   * @param percent the humidity, kept between 0 and 100
   */
  public void setHumidity(double percent) {
    report(READING, humidity.set(percent));
  }
}
