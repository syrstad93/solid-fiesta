package edu.ntnu.bidata.group1.device.controller;

import edu.ntnu.bidata.group1.utility.Role;

/**
 * Thrown when a {@link Controller} tries to do something its {@link Role} does not allow.
 */
public class AccessDeniedException extends RuntimeException {

  /**
   * Creates the exception with a message explaining what was denied.
   *
   * @param message the reason access was denied
   */
  public AccessDeniedException(String message) {
    super(message);
  }
}
