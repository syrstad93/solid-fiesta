package edu.ntnu.bidata.group1.device;

import java.io.IOException;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class Switch extends Device {

    // Oppretter en bryter som kobles til serveren
    public Switch() throws IOException, InterruptedException {
        super(Type.SENSOR);
    }

    @Override // Kalles når bryteren mottar en melding fra serveren
    public void onMessage(String message, ClientConnectionHolder sender) {
    }
}