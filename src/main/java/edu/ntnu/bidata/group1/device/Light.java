package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;

public class Light extends Device {

    protected String currentValue;

    // Oppretter en Light-enhet som kobles til serveren
    public Light() throws Exception {
        super();
    }
    // Returnerer lysets verdi
    public String readValue() {
        return currentValue;
    }

    // Kalles når en melding mottas fra serveren
    @Override
    public void onMessage(String message, ClientConnectionHolder sender) {
        System.out.println("Light: " + message);
    }
}