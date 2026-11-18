package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code Protocol} enum and the {@code MessageWorker} parser.
 *
 * <p>The tests verify that every protocol key is unique and safe to transmit,
 * that keys follow the upper-case, hyphen-separated naming rule, and that
 * lookup by key is case sensitive and trims surrounding whitespace. They also
 * cover reserved-name checks, the formatting of error replies and values, the
 * reply classification of each message, and the splitting of raw messages into
 * command keys, command values, and protocol messages.
 */
class ProtocolTest {

  /**
   * Verifies that every {@link Protocol} constant exposes a key that is unique across
   * the whole enumeration and safe to transmit.
   *
   * <p>The test iterates over all protocol values, collecting each key into a set and
   * failing if a key has already been seen. It additionally asserts that no key contains
   * the characters {@code /}, {@code :} or {@code =}, which are reserved as delimiters
   * in the message format and would otherwise corrupt parsing on the receiving side.
   */
  @Test
  void everyKeyIsUniqueAndSafeToSend() {
    Set<String> keys = new HashSet<>();
    for (Protocol protocol : Protocol.values()) {
      String key = protocol.getKey();
      assertTrue(keys.add(key), "Duplicate key: " + key);
      assertFalse(key.contains("/"), key);
      assertFalse(key.contains(":"), key);
      assertFalse(key.contains("="), key);
    }
  }

  /**
   * Verifies that every {@link Protocol} constant exposes a key that follows the naming rule:
   * the key must consist of one or more upper case letters, optionally grouped into
   * additional segments separated by single hyphens, with no leading, trailing or
   * consecutive hyphens.
   *
   * <p>The test iterates over all protocol values and fails with a descriptive message
   * identifying the offending constant and its key if any key does not match the rule.
   */
  @Test
  void everyKeyFollowsTheNamingRule() {
    for (Protocol protocol : Protocol.values()) {
      assertTrue(protocol.getKey().matches("[A-Z]+(-[A-Z]+)*"),
          protocol + " must be upper case words separated by hyphens: " + protocol.getKey());
    }
  }

  /**
   * Verifies that {@link Protocol#fromKey(String)} performs a case-sensitive lookup.
   *
   * <p>Protocol keys are defined in upper case, so passing a lower case variant of a
   * valid key must not resolve to a protocol constant. The test asserts that the
   * lookup returns an empty {@link Optional} for such a key.
   */
  @Test
  void fromKeyIsCaseSensitive() {
    assertEquals(Optional.empty(), Protocol.fromKey("room-added"));
  }

  /**
   * Verifies that {@link Protocol#isReservedName(String)} recognizes protocol keys regardless of
   * letter case.
   *
   * <p>Reserved names such as {@code ERROR} must be reported as reserved when written in upper
   * case, lower case, or mixed case, including keys that contain hyphens. Names that do not
   * correspond to a protocol key, such as {@code power}, must not be reported as reserved, and
   * {@code null} must not be treated as a reserved name.
   */
  @Test
  void protocolKeysAreReservedInAnyCase() {
    assertTrue(Protocol.isReservedName("ERROR"));
    assertTrue(Protocol.isReservedName("error"));
    assertTrue(Protocol.isReservedName("Get-Commands"));
    assertFalse(Protocol.isReservedName("power"));
    assertFalse(Protocol.isReservedName(null));
  }

  /**
   * Verifies that {@link Protocol#error(String, String)} builds an error message that begins with
   * the {@code ERROR:} prefix followed by the rejected request key.
   *
   * <p>The key is separated from the description by a comma. A {@code null} key is rendered as
   * {@code ?}, so the message still has a valid structure. Characters that would break the message
   * format, such as the slash and newline in the description, are replaced with spaces.
   */
  @Test
  void errorsStartWithTheRejectedRequest() {
    assertEquals("ERROR:ROOM-ADD,Room 3 does not exist.",
        Protocol.error("ROOM-ADD", "Room 3 does not exist."));
    assertEquals("ERROR:?,bad message", Protocol.error(null, "bad message"));
    assertEquals("ERROR:value,a b c", Protocol.error("value", "a/b\nc"));
  }

  /**
   * Verifies that {@link Protocol#fromKey(String)} resolves known protocol keys to their
   * matching {@link Protocol} constants.
   *
   * <p>The lookup accepts keys written in the canonical upper case hyphenated form, such as
   * {@code ROOM-ADDED} and {@code GET-COMMANDS}. Surrounding whitespace is ignored, so a
   * padded key such as {@code " PING "} also resolves to its constant. Keys that do not
   * correspond to any protocol constant, such as {@code power}, yield an empty
   * {@link Optional}, and a {@code null} key is likewise reported as absent rather than
   * causing an exception.
   */
  @Test
  void fromKeyFindsProtocolMessages() {
    assertEquals(Optional.of(Protocol.ROOM_ADDED), Protocol.fromKey("ROOM-ADDED"));
    assertEquals(Optional.of(Protocol.COMMANDS_REQUEST), Protocol.fromKey("GET-COMMANDS"));
    assertEquals(Optional.of(Protocol.PING), Protocol.fromKey(" PING "));
    assertEquals(Optional.empty(), Protocol.fromKey("power"));
    assertEquals(Optional.empty(), Protocol.fromKey(null));
  }

  /**
   * Verifies that {@link Protocol#withValue(Object)} combines a protocol's key and a value into a
   * single message string, with the key followed by a colon and then the value.
   *
   * <p>The test covers both a string value containing a comma and an integer value, confirming that
   * the value is appended unchanged after the colon regardless of its type.
   */
  @Test
  void withValueBuildsKeyColonValue() {
    assertEquals("ROOM-ADD:10,1", Protocol.ROOM_ADD.withValue("10,1"));
    assertEquals("COMMAND-END:2", Protocol.COMMAND_END.withValue(2));
  }

  /**
   * Verifies that {@link Protocol#isReply()} correctly classifies protocol constants.
   *
   * <p>The test asserts that {@link Protocol#ERROR} and {@link Protocol#DEVICE_ADDED} are marked as
   * replies, while {@link Protocol#STATE} and {@link Protocol#ROOM_ADD} are not.
   */
  @Test
  void repliesAreMarkedAsReplies() {
    assertTrue(Protocol.ERROR.isReply());
    assertTrue(Protocol.DEVICE_ADDED.isReply());
    assertFalse(Protocol.STATE.isReply());
    assertFalse(Protocol.ROOM_ADD.isReply());
  }

  /**
   * Verifies that {@link MessageWorker} splits a raw message into its command key and command
   * value.
   *
   * <p>The test parses a message whose command segment is {@code ROOM-ADDED:10,1}, where the text
   * before the colon is the key and the text after it is the value. It asserts that the key is
   * returned as {@code ROOM-ADDED}, that the value is returned unchanged as {@code 10,1} including
   * the embedded comma, and that the key resolves to {@link Protocol#ROOM_ADDED} through
   * {@link MessageWorker#getProtocol()}.
   */
  @Test
  void messageWorkerSplitsKeyAndValue() {
    MessageWorker mw = new MessageWorker("CH001/SE000/ROOM-ADDED:10,1/=");
    assertEquals("ROOM-ADDED", mw.getCommandKey());
    assertEquals("10,1", mw.getCommandValue());
    assertEquals(Optional.of(Protocol.ROOM_ADDED), mw.getProtocol());
  }

  /**
   * Verifies that {@link MessageWorker} correctly parses a command that carries no value.
   *
   * <p>The test parses a message whose command segment is {@code GET-COMMANDS=}, where the
   * key is followed by an equals sign but nothing after it. It asserts that the key is returned
   * as {@code GET-COMMANDS}, that the value is returned as an empty string, and that the key
   * resolves to {@link Protocol#COMMANDS_REQUEST} through {@link MessageWorker#getProtocol()}.
   */
  @Test
  void messageWorkerHandlesCommandsWithoutValue() {
    MessageWorker mw = new MessageWorker("CH001/CH002/GET-COMMANDS/=");
    assertEquals("GET-COMMANDS", mw.getCommandKey());
    assertEquals("", mw.getCommandValue());
    assertEquals(Optional.of(Protocol.COMMANDS_REQUEST), mw.getProtocol());
  }

  /**
   * Verifies that a device command embedded in a message is not treated as a protocol message.
   *
   * <p>The test parses a message whose command segment is {@code value:75}, preceded by device
   * addressing segments ({@code CH001/CH003}) and followed by an empty {@code =} segment. It
   * asserts that the command key is returned as {@code value}, that the command value is returned
   * as {@code 75}, and that {@link MessageWorker#getProtocol()} returns an empty {@link Optional},
   * since the key does not correspond to any {@link Protocol} constant.
   */
  @Test
  void deviceCommandsAreNotProtocolMessages() {
    MessageWorker mw = new MessageWorker("CH001/CH003/value:75/=");
    assertEquals("value", mw.getCommandKey());
    assertEquals("75", mw.getCommandValue());
    assertEquals(Optional.empty(), mw.getProtocol());
  }
}
