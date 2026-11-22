package edu.ntnu.bidata.group1.device.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link RequestFailedException} class.
 *
 * <p>
 * AI has assisted in writing the tests,
 * but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class RequestFailedExceptionTest {

  /**
   * Tests that the exception stores the provided message.
   */
  @Test
  public void constructorStoresMessage() {
    RequestFailedException exception =
        new RequestFailedException("Request failed.");

    assertEquals("Request failed.", exception.getMessage());
  }

  /**
   * Tests that the exception is a RuntimeException.
   */
  @Test
  public void exceptionIsRuntimeException() {
    RequestFailedException exception =
        new RequestFailedException("Request failed.");

    assertEquals(RuntimeException.class,
        exception.getClass().getSuperclass());
  }
}