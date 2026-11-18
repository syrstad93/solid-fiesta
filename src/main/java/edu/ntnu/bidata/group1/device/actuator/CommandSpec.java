package edu.ntnu.bidata.group1.device.actuator;

import edu.ntnu.bidata.group1.utility.Protocol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes one command an actuator accepts: its name, the type of its parameter, and the range
 * or options the parameter is limited to.
 *
 * <p>Actuators send their command specifications to controllers so that a controller knows what
 * it can ask a device to do. A specification is sent inside the command part of a message as a
 * comma-separated string (see {@link #toWireFormat()}):
 * <ul>
 *   <li>{@code power,BOOLEAN}</li>
 *   <li>{@code value,INTEGER,0,100}</li>
 *   <li>{@code temperature,DECIMAL,5.0,30.0}</li>
 *   <li>{@code mode,ENUM,eco|normal|boost}</li>
 *   <li>{@code toggle,NONE}</li>
 * </ul>
 *
 * <p>Names and options may not contain the characters {@code / = : , |} because those are used
 * as separators in the message protocol. A command name may not be one of the {@link Protocol}
 * keys in any letter case, such as {@code error} or {@code command}.
 */
public final class CommandSpec {

  private static final String FORBIDDEN_CHARACTERS = "/=:,|";

  private final String name;
  private final ParameterType type;
  private final double min;
  private final double max;
  private final List<String> options;

  private CommandSpec(String name, ParameterType type, double min, double max,
      List<String> options) {
    checkToken(name, "Command name");
    if (Protocol.isReservedName(name)) {
      throw new IllegalArgumentException(
          "Command name '" + name + "' is reserved by the protocol.");
    }
    if (type == null) {
      throw new IllegalArgumentException("Parameter type cannot be null.");
    }
    if (min > max) {
      throw new IllegalArgumentException(
          "Minimum (" + min + ") cannot be greater than maximum (" + max + ").");
    }
    for (String option : options) {
      checkToken(option, "Option");
    }
    if (type == ParameterType.ENUM && options.isEmpty()) {
      throw new IllegalArgumentException("An ENUM command needs at least one option.");
    }
    this.name = name;
    this.type = type;
    this.min = min;
    this.max = max;
    this.options = Collections.unmodifiableList(new ArrayList<>(options));
  }

  /**
   * Creates a command that takes no parameter.
   *
   * @param name the command name
   * @return the command specification
   */
  public static CommandSpec noParameter(String name) {
    return new CommandSpec(name, ParameterType.NONE, 0, 0, List.of());
  }

  /**
   * Creates a command that takes {@code true} or {@code false}.
   *
   * @param name the command name
   * @return the command specification
   */
  public static CommandSpec bool(String name) {
    return new CommandSpec(name, ParameterType.BOOLEAN, 0, 0, List.of());
  }

  /**
   * Creates a command that takes a whole number in the inclusive range {@code min..max}.
   *
   * @param name the command name
   * @param min the smallest accepted value
   * @param max the largest accepted value
   * @return the command specification
   */
  public static CommandSpec integer(String name, long min, long max) {
    return new CommandSpec(name, ParameterType.INTEGER, min, max, List.of());
  }

  /**
   * Creates a command that takes a decimal number in the inclusive range {@code min..max}.
   *
   * @param name the command name
   * @param min the smallest accepted value
   * @param max the largest accepted value
   * @return the command specification
   */
  public static CommandSpec decimal(String name, double min, double max) {
    return new CommandSpec(name, ParameterType.DECIMAL, min, max, List.of());
  }

  /**
   * Creates a command that takes one of the given options.
   *
   * @param name the command name
   * @param options the accepted values
   * @return the command specification
   */
  public static CommandSpec options(String name, String... options) {
    return new CommandSpec(name, ParameterType.ENUM, 0, 0, List.of(options));
  }

  /**
   * Checks that the given argument is acceptable for this command.
   *
   * @param argument the argument as received in a message; ignored for {@link ParameterType#NONE}
   * @throws IllegalArgumentException with a short reason if the argument is not acceptable
   */
  public void validate(String argument) {
    if (type == ParameterType.NONE) {
      return;
    }
    if (argument == null || argument.isBlank()) {
      throw new IllegalArgumentException(name + " needs a " + type + " parameter");
    }
    String value = argument.trim();
    switch (type) {
      case BOOLEAN -> {
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
          throw new IllegalArgumentException(name + " expects true or false");
        }
      }
      case INTEGER -> {
        long number;
        try {
          number = Long.parseLong(value);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(name + " expects a whole number");
        }
        checkRange(number);
      }
      case DECIMAL -> {
        double number;
        try {
          number = Double.parseDouble(value);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(name + " expects a decimal number");
        }
        if (Double.isNaN(number)) {
          throw new IllegalArgumentException(name + " expects a decimal number");
        }
        checkRange(number);
      }
      case ENUM -> {
        if (!options.contains(value)) {
          throw new IllegalArgumentException(
              name + " expects one of " + String.join(" ", options));
        }
      }
      default -> throw new IllegalStateException("Unhandled parameter type " + type);
    }
  }

  /**
   * Returns whether the given argument is acceptable for this command.
   *
   * @param argument the argument to check
   * @return {@code true} if {@link #validate(String)} would accept it
   */
  public boolean isValid(String argument) {
    try {
      validate(argument);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Returns this specification as the string sent inside a message.
   *
   * @return for example {@code value,INTEGER,0,100}
   */
  public String toWireFormat() {
    return switch (type) {
      case NONE, BOOLEAN -> name + "," + type;
      case INTEGER -> name + "," + type + "," + (long) min + "," + (long) max;
      case DECIMAL -> name + "," + type + "," + min + "," + max;
      case ENUM -> name + "," + type + "," + String.join("|", options);
    };
  }

  /**
   * Reads a specification from the string produced by {@link #toWireFormat()}.
   *
   * @param wire the string received in a message
   * @return the command specification
   * @throws IllegalArgumentException if the string is not a valid specification
   */
  public static CommandSpec fromWireFormat(String wire) {
    if (wire == null) {
      throw new IllegalArgumentException("Command specification cannot be null.");
    }
    String[] parts = wire.trim().split(",", -1);
    if (parts.length < 2) {
      throw new IllegalArgumentException("Malformed command specification: " + wire);
    }
    String name = parts[0];
    ParameterType type;
    try {
      type = ParameterType.valueOf(parts[1]);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown parameter type in: " + wire);
    }
    try {
      return switch (type) {
        case NONE -> {
          expectParts(parts, 2, wire);
          yield noParameter(name);
        }
        case BOOLEAN -> {
          expectParts(parts, 2, wire);
          yield bool(name);
        }
        case INTEGER -> {
          expectParts(parts, 4, wire);
          yield integer(name, Long.parseLong(parts[2]), Long.parseLong(parts[3]));
        }
        case DECIMAL -> {
          expectParts(parts, 4, wire);
          yield decimal(name, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        }
        case ENUM -> {
          expectParts(parts, 3, wire);
          yield options(name, parts[2].split("\\|", -1));
        }
      };
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Malformed range in: " + wire);
    }
  }

  /**
   * Returns the command's name.
   *
   * @return for example {@code power}
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the type of the command's parameter.
   *
   * @return the parameter type
   */
  public ParameterType getType() {
    return type;
  }

  /**
   * Returns the smallest accepted value. Only meaningful for INTEGER and DECIMAL commands.
   *
   * @return the minimum
   */
  public double getMin() {
    return min;
  }

  /**
   * Returns the largest accepted value. Only meaningful for INTEGER and DECIMAL commands.
   *
   * @return the maximum
   */
  public double getMax() {
    return max;
  }

  /**
   * Returns the accepted values. Empty unless this is an ENUM command.
   *
   * @return an unmodifiable list of options
   */
  public List<String> getOptions() {
    return options;
  }

  private void checkRange(double number) {
    if (number < min || number > max) {
      String range = type == ParameterType.INTEGER
          ? (long) min + " and " + (long) max
          : min + " and " + max;
      throw new IllegalArgumentException(name + " must be between " + range);
    }
  }

  private static void expectParts(String[] parts, int expected, String wire) {
    if (parts.length != expected) {
      throw new IllegalArgumentException("Malformed command specification: " + wire);
    }
  }

  private static void checkToken(String token, String what) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException(what + " cannot be empty.");
    }
    for (char c : FORBIDDEN_CHARACTERS.toCharArray()) {
      if (token.indexOf(c) >= 0) {
        throw new IllegalArgumentException(
            what + " cannot contain '" + c + "': " + token);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommandSpec other)) {
      return false;
    }
    return Double.compare(min, other.min) == 0
        && Double.compare(max, other.max) == 0
        && name.equals(other.name)
        && type == other.type
        && options.equals(other.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, min, max, options);
  }

  @Override
  public String toString() {
    return toWireFormat();
  }
}
