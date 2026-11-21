package edu.ntnu.bidata.group1.device.sensor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TemperatureSensor}.
 *
 * <p>The tests verify that temperature sensors can be created using both
 * constructors, that the reading name constant contains the expected value,
 * and that temperatures throughout the supported range are accepted.
 */
class TemperatureSensorTest {

    /**
     * Verifies that the default constructor creates a valid sensor instance.
     *
     * <p>The constructor generates a random sensor name and should create
     * a non-null object.
     */
    @Test
    void defaultConstructorCreatesSensor() {
        TemperatureSensor sensor = new TemperatureSensor();

        assertNotNull(sensor);
    }

    /**
     * Verifies that a sensor can be created with a custom name.
     *
     * <p>The sensor should accept a valid name and create a non-null instance.
     */
    @Test
    void constructorWithNameCreatesSensor() {
        TemperatureSensor sensor =
                new TemperatureSensor("living-room-temperature");

        assertNotNull(sensor);
    }

    /**
     * Verifies that the reading constant contains the expected protocol name.
     *
     * <p>The temperature sensor should report values using the reading
     * identifier {@code temperature}.
     */
    @Test
    void readingConstantHasExpectedValue() {
        assertEquals("temperature", TemperatureSensor.READING);
    }

    /**
     * Verifies that temperatures within the supported range are accepted.
     *
     * <p>The test sets a typical indoor temperature and expects no exception
     * to be thrown.
     */
    @Test
    void acceptsTemperatureWithinRange() {
        TemperatureSensor sensor = new TemperatureSensor("test");

        assertDoesNotThrow(() -> sensor.setTemperature(25.0));
    }

    /**
     * Verifies that the minimum supported temperature is accepted.
     *
     * <p>The documented lower limit is -10 °C.
     */
    @Test
    void acceptsMinimumTemperature() {
        TemperatureSensor sensor = new TemperatureSensor("test");

        assertDoesNotThrow(() -> sensor.setTemperature(-10.0));
    }

    /**
     * Verifies that the maximum supported temperature is accepted.
     *
     * <p>The documented upper limit is 40 °C.
     */
    @Test
    void acceptsMaximumTemperature() {
        TemperatureSensor sensor = new TemperatureSensor("test");

        assertDoesNotThrow(() -> sensor.setTemperature(40.0));
    }
}