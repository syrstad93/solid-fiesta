package edu.ntnu.bidata.group1.device.controller;

/**
 * The reason a {@link Controller} request's future failed: the server refused the request, or
 * did not answer in time.
 */
public class RequestFailedException extends RuntimeException {

  /**
   * Creates the exception with a message explaining why the request failed.
   *
   * @param message why the request failed, for example the server's reason
   */
  public RequestFailedException(String message) {
    super(message);
  }
}
