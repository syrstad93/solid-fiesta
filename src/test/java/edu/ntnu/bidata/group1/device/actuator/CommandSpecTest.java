package edu.ntnu.bidata.group1.device.actuator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@code CommandSpec}, verifying parameter validation for each supported
 * parameter type, the round-trip serialization of specifications to and from their message
 * representation, and the rejection of malformed or reserved specifications.
 */
class CommandSpecTest {

  /**
   * Verifies that an integer {@link CommandSpec} created with the range 0 to 100
   * accepts only whole-number values that fall within that range, inclusive of
   * both bounds.
   *
   * <p>Values at the lower and upper bounds, and a value surrounded by surrounding
   * whitespace, are considered valid. Values below the minimum, above the maximum,
   * containing a fractional part, non-numeric text, an empty string, or a null
   * argument are rejected.
   */
  @Test
  void integerAcceptsValuesInsideRangeOnly() {
    CommandSpec spec = CommandSpec.integer("value", 0, 100);
    assertTrue(spec.isValid("0"));
    assertTrue(spec.isValid("100"));
    assertTrue(spec.isValid(" 42 "));
    assertFalse(spec.isValid("-1"));
    assertFalse(spec.isValid("101"));
    assertFalse(spec.isValid("4.5"));
    assertFalse(spec.isValid("abc"));
    assertFalse(spec.isValid(""));
    assertFalse(spec.isValid(null));
  }

  /**
   * Verifies that a decimal {@link CommandSpec} created with the range 5.0 to 30.0
   * accepts only numeric values that fall within that range, inclusive of both bounds.
   *
   * <p>The values "21.5" and "5" are expected to be valid, since the first lies strictly
   * inside the range and the second equals the lower bound. The value "30.1", which
   * exceeds the upper bound, and the non-finite text "NaN" are expected to be rejected.
   */
  @Test
  void decimalAcceptsValuesInsideRangeOnly() {
    CommandSpec spec = CommandSpec.decimal("temperature", 5.0, 30.0);
    assertTrue(spec.isValid("21.5"));
    assertTrue(spec.isValid("5"));
    assertFalse(spec.isValid("30.1"));
    assertFalse(spec.isValid("NaN"));
  }

  /**
   * Verifies that a boolean {@link CommandSpec} created with {@link CommandSpec#bool(String)}
   * accepts only the values "true" and "false", ignoring letter case, and rejects every other
   * input.
   *
   * <p>The specification is created with the name "power". The values "true" and "FALSE" are
   * expected to be valid, while the values "yes" and "1" are expected to be rejected.
   */
  @Test
  void booleanAcceptsOnlyTrueOrFalse() {
    CommandSpec spec = CommandSpec.bool("power");
    assertTrue(spec.isValid("true"));
    assertTrue(spec.isValid("FALSE"));
    assertFalse(spec.isValid("yes"));
    assertFalse(spec.isValid("1"));
  }

  /**
   * Verifies that an enumerated {@link CommandSpec} created with a fixed list of options
   * accepts only those listed options and rejects any other value.
   *
   * <p>The specification is created with the name "mode" and the choices "eco", "normal" and
   * "boost". The value "eco" is expected to be valid, while the value "turbo", which is not
   * among the listed choices, is expected to be rejected.
   */
  @Test
  void enumAcceptsOnlyListedOptions() {
    CommandSpec spec = CommandSpec.options("mode", "eco", "normal", "boost");
    assertTrue(spec.isValid("eco"));
    assertFalse(spec.isValid("turbo"));
  }

  /**
   * Verifies that a no-parameter {@link CommandSpec} ignores whatever argument is supplied
   * to {@link CommandSpec#isValid(String)}.
   *
   * <p>A command created with {@link CommandSpec#noParameter(String)} takes no arguments, so
   * validity must not depend on the value passed in. Both a {@code null} argument and an
   * empty string are expected to be reported as valid.
   */
  @Test
  void noParameterIgnoresArgument() {
    CommandSpec spec = CommandSpec.noParameter("toggle");
    assertTrue(spec.isValid(null));
    assertTrue(spec.isValid(""));
  }

  /**
   * Verifies that the {@link IllegalArgumentException} thrown by
   * {@link CommandSpec#validate(String)} for an out-of-range value describes the problem in a
   * human-readable way.
   *
   * <p>An integer {@link CommandSpec} named "value" with a range from 0 to 100 is validated against
   * the input "150", which lies above the maximum. The exception message must state the constraint
   * that was violated, namely that the value must be between the minimum and the maximum bounds.
   */
  @Test
  void validateExplainsTheProblem() {
    CommandSpec spec = CommandSpec.integer("value", 0, 100);
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> spec.validate("150"));
    assertEquals("value must be between 0 and 100", e.getMessage());
  }

  /**
   * Verifies that every supported {@link CommandSpec} type survives a round trip through its
   * message representation.
   *
   * <p>Each specification in the test set, covering a no-parameter command, a boolean command, an
   * integer command with a range, a decimal command with a range, and an options command, is
   * serialized with {@link CommandSpec#toMessageValue()} and then parsed back with
   * {@link CommandSpec#fromMessageValue(String)}. The reconstructed specification must be equal to
   * the original one.
   */
  @Test
  void messageValueRoundTrips() {
    List<CommandSpec> specs = List.of(
        CommandSpec.noParameter("toggle"),
        CommandSpec.bool("power"),
        CommandSpec.integer("value", 0, 100),
        CommandSpec.decimal("temperature", 5.0, 30.5),
        CommandSpec.options("mode", "eco", "normal", "boost"));
    for (CommandSpec spec : specs) {
      assertEquals(spec, CommandSpec.fromMessageValue(spec.toMessageValue()));
    }
  }

  /**
   * Verifies that {@link CommandSpec#toMessageValue()} produces the documented wire format for each
   * supported specification type.
   *
   * <p>An integer specification is rendered as its name, the type {@code INTEGER}, and its minimum
   * and maximum bounds, separated by commas. A boolean specification is rendered as its name and
   * the type {@code BOOLEAN}. An options specification is rendered as its name, the type
   * {@code ENUM}, and its choices joined by a pipe character ("|").
   */
  @Test
  void messageValueLooksAsDocumented() {
    assertEquals("value,INTEGER,0,100", CommandSpec.integer("value", 0, 100).toMessageValue());
    assertEquals("power,BOOLEAN", CommandSpec.bool("power").toMessageValue());
    assertEquals("mode,ENUM,eco|boost", CommandSpec.options("mode", "eco", "boost")
        .toMessageValue());
  }

  /**
   * Verifies that the message value produced by a decimal {@link CommandSpec} does not
   * contain any of the characters used as message separators in the protocol, namely
   * the slash ("/"), the equals sign ("="), and the colon (":").
   *
   * <p>The check uses a decimal specification with a range from -10.0 to 30.0 and asserts
   * that its {@link CommandSpec#toMessageValue()} result is free of these reserved
   * characters, so the value can be embedded in a message without being mistaken for
   * structural delimiters.
   */
  @Test
  void messageValueHasNoMessageSeparators() {
    String value = CommandSpec.decimal("temperature", -10.0, 30.0).toMessageValue();
    assertFalse(value.contains("/"));
    assertFalse(value.contains("="));
    assertFalse(value.contains(":"));
  }

  /**
   * Verifies that malformed message values are rejected by
   * {@link CommandSpec#fromMessageValue(String)} with an {@link IllegalArgumentException}.
   *
   * <p>A message value must consist of a name followed by a type and, where applicable, its
   * parameters. Values that consist only of a name, values that name an unknown type such as
   * "WHATEVER", values with an integer specification missing its range bound, values with surplus
   * fields, and a null argument are all considered invalid and must fail with an
   * {@link IllegalArgumentException}.
   */
  @Test
  void malformedMessageValueIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.fromMessageValue("value"));
    assertThrows(IllegalArgumentException.class,
        () -> CommandSpec.fromMessageValue("value,WHATEVER"));
    assertThrows(IllegalArgumentException.class,
        () -> CommandSpec.fromMessageValue("value,INTEGER,0"));
    assertThrows(IllegalArgumentException.class,
        () -> CommandSpec.fromMessageValue("value,INTEGER,a,b"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.fromMessageValue(null));
  }

  /**
   * Verifies that invalid {@link CommandSpec} definitions are rejected with an
   * {@link IllegalArgumentException}.
   *
   * <p>Rejected definitions include an integer specification whose minimum is greater
   * than its maximum, boolean specifications whose names contain a path separator
   * ("/") or a colon (":"), a blank boolean name, an options specification without
   * any choices, and an options specification containing a choice with the reserved
   * separator character ("|"). A specification whose minimum equals its maximum is
   * considered valid.
   */
  @Test
  void invalidSpecificationsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.integer("value", 10, 0));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.bool("a/b"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.bool("a:b"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.bool(" "));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.options("mode"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.options("mode", "a|b"));
    assertDoesNotThrow(() -> CommandSpec.integer("value", 5, 5));
  }

  /**
   * Verifies that protocol keys cannot be used as command names when creating a
   * {@link CommandSpec}.
   *
   * <p>Command names must not collide with reserved protocol keywords, regardless of letter case.
   * Attempting to create a command specification whose name matches a protocol key, such as
   * "error" or "unsubscribe", must fail with an {@link IllegalArgumentException}. This applies
   * to each supported factory method, including boolean, no-parameter, and integer specifications.
   */
  @Test
  void protocolKeysCannotBeCommandNames() {
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.bool("error"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.noParameter("unsubscribe"));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.integer("hello", 0, 9));
    assertThrows(IllegalArgumentException.class, () -> CommandSpec.bool("ERROR"));
  }
}
