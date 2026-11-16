package edu.ntnu.bidata.group1.device.Sensor;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class TemperatureSensor extends Device {

    /**
     * Stores the most recently received temperature value.
     */
    protected String currentValue;

    /**
     * Creates a temperature sensor and connects it to the server.
     */
    public TemperatureSensor() throws Exception {
        super();
    }

    /**
     * Returns the current temperature value.
     *
     * @return the current temperature value.
     */
    public String readValue() {
        return currentValue;
    }

    /**
     * Called automatically when the temperature sensor receives a message from the server.
     *
     * @param message the received message.
     * @param sender the connection that sent the message.
     */
    @Override
    public void onMessage(String message, ClientConnectionHolder sender) {
        currentValue = message;
        System.out.println("Temperature: " + message);
    }
}