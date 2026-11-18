package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MessageWorker}, verifying address formatting, address validation,
 * message construction, and message parsing behavior.
 */
class MessageWorkerTest {

  /**
   * Verifies that {@link MessageWorker#address(String, long)} formats addresses with a
   * zero-padded numeric part of at least three digits.
   *
   * <p>Numbers with fewer than three digits are left-padded with zeros, numbers with exactly
   * three digits are unchanged, and numbers with more than three digits are formatted in full
   * without truncation. The predefined constants {@link MessageWorker#SERVER} and
   * {@link MessageWorker#BROADCAST} are also checked to ensure they follow the same
   * three-digit format.
   */
  @Test
  void addressesHaveThreeDigits() {
    assertEquals("CH001", MessageWorker.address("CH", 1));
    assertEquals("RO010", MessageWorker.address("RO", 10));
    assertEquals("CH123", MessageWorker.address("CH", 123));
    assertEquals("CH1234", MessageWorker.address("CH", 1234));
    assertEquals("SE000", MessageWorker.SERVER);
    assertEquals("AL000", MessageWorker.BROADCAST);
  }

  /**
   * Verifies that {@link MessageWorker#address(String, long)} rejects invalid input.
   * A {@code null} prefix and a negative numeric identifier are each expected to cause an
   * {@link IllegalArgumentException} to be thrown.
   */
  @Test
  void invalidAddressesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> MessageWorker.address(null, 1));
    assertThrows(IllegalArgumentException.class, () -> MessageWorker.address("CH", -1));
  }

  /**
   * Verifies that {@link MessageWorker#buildMessage()} formats the destination and source
   * addresses with a three-digit numeric part, so that a destination ID of 9 and a source ID
   * of 42 are written as zero-padded three-digit numbers. The test also checks that the
   * command and the message terminator are appended after the addresses, and that the
   * resulting message matches the expected string.
   */
  @Test
  void builtMessagesUseThreeDigits() {
    MessageWorker mw = new MessageWorker()
        .setDestination("CH", 9)
        .setSource("RO", 42)
        .setCommand("power:true")
        .buildMessage();
    assertEquals("CH009/RO042/power:true/=", mw.getMessage());
  }

  /**
   * Verifies that {@link MessageWorker} accepts addresses with any number of digits when parsing
   * a message. The numeric part of the destination address is read as the same identifier whether
   * it has no leading zeros, leading zeros, or more digits than usual. The source address with
   * the value {@code 000} is read as the identifier {@code 0}.
   */
  @Test
  void anyNumberOfDigitsIsAcceptedWhenReading() {
    assertEquals(Long.valueOf(1), new MessageWorker("CH1/SE000/PING/=").getDestinationId());
    assertEquals(Long.valueOf(1), new MessageWorker("CH01/SE000/PING/=").getDestinationId());
    assertEquals(Long.valueOf(1), new MessageWorker("CH001/SE000/PING/=").getDestinationId());
    assertEquals(Long.valueOf(1234), new MessageWorker("CH1234/SE000/PING/=").getDestinationId());
    assertEquals(Long.valueOf(0), new MessageWorker("CH001/SE000/PING/=").getSourceId());
  }
}
