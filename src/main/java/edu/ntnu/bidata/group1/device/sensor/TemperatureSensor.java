package edu.ntnu.bidata.group1.device.sensor;

/**
 * Sensor that measures air temperature in degrees Celsius.
 *
 * <p>Reports the reading {@value #READING}. The simulated temperature starts at 21 °C and
 * drifts by up to 0.3 °C per measurement, between -10 and 40 °C.
 */
public class TemperatureSensor extends Sensor {

  /** Name of the reading this sensor reports. */
  public static final String READING = "temperature";

  private final SimulatedValue temperature = new SimulatedValue(21.0, -10.0, 40.0, 0.3);

  /**
   * Creates a temperature sensor with a random name, such as
   * {@code temperature-sensor-3f9a1c2e}. See {@link #TemperatureSensor(String)}.
   */
  public TemperatureSensor() {
    this(randomName("temperature-sensor"));
  }

  /**
   * Creates a temperature sensor. Call {@link #start()} to connect it and start
   * measuring.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code living-room-temperature}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public TemperatureSensor(String name) {
    super(name);
  }

  @Override
  protected void measure() {
    report(READING, temperature.next());
  }

  /**
   * Sets the simulated temperature and reports it right away. Useful for demos and tests, for
   * example to trigger an automation.
   *
   * @param celsius the temperature, kept between -10 and 40
   */
  public void setTemperature(double celsius) {
    report(READING, temperature.set(celsius));
  }
}
