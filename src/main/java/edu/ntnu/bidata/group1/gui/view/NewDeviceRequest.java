package edu.ntnu.bidata.group1.gui.view;

/**
 * What the owner filled in to create a new device, as typed. The GUI controller checks it,
 * creates the device and registers it.
 *
 * @param type the device type, as shown in the drop-down, for example {@code Light}; or
 *     {@code null} if none is chosen
 * @param name the device's name; blank for a random name
 * @param room the room to put the device in, or {@code null} for none
 */
public record NewDeviceRequest(String type, String name, Choice room) {
}
