package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class Light extends Device {

    /**
     * Stores the most recently received light value.
     */

    protected String currentValue;
    public Light() throws Exception {
        super();
    }

/**
 * Returns the current light value.
 *
 * @return the current light value.
 */
    
    public String readValue() {
        return currentValue;
    }

/**
 * Called automatically when the light device receives a message from the server.
 *
 * @param message the received message.
 * @param sender the connection that sent the message.
 */

    @Override
    public void onMessage(String message, ClientConnectionHolder sender) {
        System.out.println("Light: " + message);
    }
}