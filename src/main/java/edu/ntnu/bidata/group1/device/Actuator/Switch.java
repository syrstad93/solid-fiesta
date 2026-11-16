package edu.ntnu.bidata.group1.device.Actuator;

import java.io.IOException;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class Switch extends Device {

/**
 * Creates a switch device and connects it to the server.
 */
    public Switch() throws IOException, InterruptedException {
        super();
    }

/**
 * Called automatically when the switch receives a message from the server.
 *
 * @param message the received message.
 * @param sender the connection that sent the message.
 */

    @Override // Kalles når bryteren mottar en melding fra serveren
    public void onMessage(String message, ClientConnectionHolder sender) {
    }
}