package edu.ntnu.bidata.group1.device.sensor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link HumiditySensor} class.
 *
 * <p>
 * AI has assisted in writing the tests,
 * but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class HumiditySensorTest {

  /**
   * Tests that the default constructor creates a sensor.
   */
  @Test
  public void defaultConstructorCreatesSensor() {
    HumiditySensor sensor = new HumiditySensor();

    assertNotNull(sensor);
  }

  /**
   * Tests that the named constructor creates a sensor.
   */
  @Test
  public void namedConstructorCreatesSensor() {
    HumiditySensor sensor = new HumiditySensor("Living Room");

    assertNotNull(sensor);
  }

  /**
   * Tests that humidity can be set within the valid range.
   */
  @Test
  public void setHumidityAcceptsValidValue() {
    HumiditySensor sensor = new HumiditySensor();

    assertDoesNotThrow(() -> sensor.setHumidity(50));
  }

  /**
   * Tests that humidity values below the minimum are handled.
   */
  @Test
  public void setHumidityAcceptsValueBelowMinimum() {
    HumiditySensor sensor = new HumiditySensor();

    assertDoesNotThrow(() -> sensor.setHumidity(-10));
  }

  /**
   * Tests that humidity values above the maximum are handled.
   */
  @Test
  public void setHumidityAcceptsValueAboveMaximum() {
    HumiditySensor sensor = new HumiditySensor();

    assertDoesNotThrow(() -> sensor.setHumidity(150));
  }
}