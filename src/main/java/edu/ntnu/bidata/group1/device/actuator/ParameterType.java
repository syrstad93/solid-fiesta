package edu.ntnu.bidata.group1.device.actuator;

/**
 * The kind of value an actuator command takes as its parameter.
 *
 * <p>Each {@link CommandSpec} has one parameter type. The type decides how the command's argument
 * is validated before it is executed, and whether a range or a set of options applies.
 */
public enum ParameterType {

  /** The command takes no parameter, for example {@code toggle}. */
  NONE,

  /** The parameter is {@code true} or {@code false}. */
  BOOLEAN,

  /** The parameter is a whole number, limited to an inclusive range. */
  INTEGER,

  /** The parameter is a decimal number, limited to an inclusive range. */
  DECIMAL,

  /** The parameter is one value out of a fixed list of options. */
  ENUM
}
