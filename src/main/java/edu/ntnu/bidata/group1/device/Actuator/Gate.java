package edu.ntnu.bidata.group1.device.Actuator;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import java.io.IOException;

public class Gate extends Device {

    /**
     * Creates a gate device and connects it to the server.
     */
    public Gate() throws IOException, InterruptedException {
        super();
    }

    /**
     * Called automatically when the gate receives a message from the server.
     *
     * @param message the received message.
     * @param sender the connection that sent the message.
     */
    @Override
    public void onMessage(String message, ClientConnectionHolder sender) {
    }
}