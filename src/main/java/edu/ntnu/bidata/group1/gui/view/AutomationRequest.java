package edu.ntnu.bidata.group1.gui.view;

import edu.ntnu.bidata.group1.utility.Condition;

/**
 * What the user filled in to add an automation, as typed. The GUI controller checks it and
 * passes it to the {@link edu.ntnu.bidata.group1.device.controller.Controller}.
 *
 * @param sensor the device to watch, or {@code null} if none is chosen
 * @param reading the reading to watch, for example {@code temperature}
 * @param condition above or below, or {@code null} if none is chosen
 * @param threshold the threshold, as typed
 * @param device the device to control, or {@code null} if none is chosen
 * @param command the command to send, for example {@code power}
 * @param argument the command's argument, as typed
 */
public record AutomationRequest(Choice sensor, String reading, Condition condition,
    String threshold, Choice device, String command, String argument) {
}
