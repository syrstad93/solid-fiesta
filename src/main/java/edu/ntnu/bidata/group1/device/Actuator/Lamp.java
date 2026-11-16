package edu.ntnu.bidata.group1.device.Actuator;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class Lamp extends Device {

    /**
     * Stores the current state of the lamp.
     */
    protected String currentValue;

    /**
     * Creates a lamp actuator and connects it to the server.
     */
    public Lamp() throws Exception {
        super();
    }

    /**
     * Returns the current state of the lamp.
     *
     * @return the current state of the lamp.
     */
    public String readValue() {
        return currentValue;
    }

    /**
     * Called automatically when the lamp receives a message from the server.
     *
     * @param message the received message.
     * @param sender the connection that sent the message.
     */
    @Override
    public void onMessage(String message, ClientConnectionHolder sender) {
        System.out.println("Lamp: " + message);
    }
}