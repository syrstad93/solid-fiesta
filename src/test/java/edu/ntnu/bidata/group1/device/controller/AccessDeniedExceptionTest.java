package edu.ntnu.bidata.group1.device.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link AccessDeniedException} class.
 *
 * <p>
 * AI has assisted in writing the tests,
 * but the tests have been manually verified to be correct and relevant.
 * </p>
 */
public class AccessDeniedExceptionTest {

  /**
   * Tests that the exception stores the provided message.
   */
  @Test
  public void constructorStoresMessage() {
    AccessDeniedException exception =
        new AccessDeniedException("Access denied.");

    assertEquals("Access denied.", exception.getMessage());
  }

  /**
   * Tests that the exception is a RuntimeException.
   */
  @Test
  public void exceptionIsRuntimeException() {
    AccessDeniedException exception =
        new AccessDeniedException("Access denied.");

    assertEquals(RuntimeException.class,
        exception.getClass().getSuperclass());
  }
}