package edu.ntnu.bidata.group1.device.controller;

/**
 * Something a {@link Controller} may be allowed to do. Which permissions a controller has is
 * decided by its {@link Role}.
 *
 * <p>Viewing devices, their commands and their state is not a permission: every role can do it.
 */
public enum Permission {

  /** Send commands to controlled devices, such as {@code power:true} or {@code value:75}. */
  CONTROL_DEVICES,

  /** Add controlled devices to rooms and remove them from rooms. */
  MANAGE_ROOMS,

  /** Add devices to the controller and remove them from it. */
  MANAGE_DEVICES,

  /** Add and remove automations: rules the server follows when a sensor crosses a threshold. */
  MANAGE_AUTOMATIONS
}
