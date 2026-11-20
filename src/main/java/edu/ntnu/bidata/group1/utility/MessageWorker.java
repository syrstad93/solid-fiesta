package edu.ntnu.bidata.group1.utility;

import edu.ntnu.bidata.group1.network.server.ClientHandler;
import edu.ntnu.bidata.group1.network.server.Server;
import java.util.Optional;

/**
 * Manages a transmission message,
 * and contains methods for setting, building, editing and returning the specific parts.
 *
 * <p>A command message is a segmented {@link String} with the below structure.
 * The separation between each segment is indicated by a {@code /}.
 *
 * <p>{@code Destination/Source/Command/End of transmission}
 *
 * <p>{@code Destination} contains a prefix, followed by the ID of the recipient if relevant.
 * These are the possible prefixes:
 * <ul>
 *   <li>
 *     {@code CH}: a {@link ClientHandler}. Is followed by the ID of the client handler.
 *   </li>
 *   <li>
 *     {@code SE}: the {@link Server}. Is followed by placeholder ID {@code 000}, or no ID at all.
 *   </li>
 *   <li>
 *     {@code RO}: a room of clients. Is followed by the ID of the room.
 *   </li>
 *   <li>
 *     {@code AL}: broadcast to all. Is followed by placeholder ID {@code 000}, or no ID at all.
 *   </li>
 * </ul>
 *
 * <p>{@code Source} contains a prefix, followed by the ID of the sender if relevant.
 * These are the possible prefixes:
 * <ul>
 *   <li>
 *     {@code CH}: a {@link ClientHandler}. Is followed by the ID of the client handler.
 *   </li>
 *   <li>
 *     {@code SE}: the {@link Server}. Is followed by placeholder ID {@code 000}, or no ID at all.
 *   </li>
 * </ul>
 *
 * <p>IDs are written with {@value #ID_DIGITS} digits, padded with leading zeros: client 1 is
 * {@code CH001} and room 10 is {@code RO010}. IDs of 1000 and more are written in full. When
 * reading a message, any number of digits is accepted, so {@code CH1}, {@code CH01} and
 * {@code CH001} all mean client 1. Use {@link #address(String, long)} to build an address.
 *
 * <p>{@code Command} is the command information for the destination device.
 * This part of the message is specific for the device.
 *
 * <p>{@code End of transmission} is the symbol {@code =}, which signifies the end of the message.
 */
public class MessageWorker {

  /** How many digits an ID is written with in an address, padded with leading zeros. */
  public static final int ID_DIGITS = 3;

  /** Address of the server: {@code SE000}. */
  public static final String SERVER = address("SE", 0);

  /** Address that reaches every connected client: {@code AL000}. */
  public static final String BROADCAST = address("AL", 0);

  private String message;

  private String destination;
  private String source;
  private String command;

  /**
   * Instantiates this class by setting the {@link #message} to the given {@code message},
   * and then interprets the message into its specific parts,
   * which are set to {@link #destination}, {@link #source}, and {@link #command}.
   *
   * @param message is a command message.
   */
  public MessageWorker(String message) {
    setMessage(message);
  }

  /**
   * Instantiates the class without setting a message. The message can be built later using the
   * setter methods for {@link #destination}, {@link #source}, and {@link #command},
   * or by using the setter method for {@link #message} to set a full message immediately.
   */
  public MessageWorker() {
    // Empty constructor.
  }

  /**
   * Sets the full message to {@link #message},
   * and then interprets the message into its specific parts,
   * which are set to {@link #destination}, {@link #source}, and {@link #command}.
   *
   * @param message is a command message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   * @throws IllegalArgumentException if {@code message} does not contain a valid
   *        destination prefix, which is either {@code CH}, {@code SE}, {@code RO}, or {@code AL}.
   * @throws IllegalArgumentException if {@code message} does not contain exactly three {@code /}
   *        characters to separate the parts.
   * @throws IllegalArgumentException if {@code message} does not end with the indication that the
   *        message has ended ('=').
   */
  public void setMessage(String message) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }
    if (!message.contains("CH")
        && !message.contains("SE")
        && !message.contains("RO")
        && !message.contains("AL")) {
      throw new IllegalArgumentException(
          "Message must contain a valid destination prefix: 'CH', 'SE', 'RO', or 'AL'."
      );
    }
    if (message.length() - message.replace("/", "").length() != 3) {
      throw new IllegalArgumentException(
          "Message must contain exactly three '/' characters to separate the parts."
      );
    }
    if (!message.endsWith("=")) {
      throw new IllegalArgumentException(
          "Message must end with the end-of-message character ('=')."
      );
    }
    this.message = message;
    interpretMessage();
  }

  /**
   * Builds an address from a prefix and an ID, with the ID padded to {@value #ID_DIGITS} digits.
   *
   * @param prefix is the prefix, {@code CH}, {@code SE}, {@code RO} or {@code AL}.
   * @param id is the ID; must not be negative.
   * @return the address, for example {@code CH001} for prefix {@code CH} and ID 1.
   * @throws IllegalArgumentException if {@code prefix} is {@code null} or {@code id} is negative.
   */
  public static String address(String prefix, long id) {
    if (prefix == null) {
      throw new IllegalArgumentException("Prefix cannot be null.");
    }
    if (id < 0) {
      throw new IllegalArgumentException("ID cannot be negative.");
    }
    return prefix + String.format("%0" + ID_DIGITS + "d", id);
  }

  /**
   * Sets the destination part of the message to {@link #destination}.
   *
   * @param destination is the destination part of the message.
   * @return this instance of {@link MessageWorker}.
   * @throws IllegalArgumentException if {@code destination} is {@code null}.
   */
  public MessageWorker setDestination(String destination) {
    if (destination == null) {
      throw new IllegalArgumentException("Destination cannot be null.");
    }
    this.destination = destination;
    return this;
  }

  /**
   * Sets the destination part of the message to {@link #destination}.
   *
   * @param prefix is the prefix of the destination part of the message.
   * @param id is the ID of the destination part of the message.
   * @return this instance of {@link MessageWorker}.
   * @throws IllegalArgumentException if {@code destination} is {@code null}.
   */
  public MessageWorker setDestination(String prefix, long id) {
    if (prefix == null) {
      throw new IllegalArgumentException("Destination cannot be null.");
    }

    this.destination = address(prefix, id);
    return this;
  }

  /**
   * Sets the source part of the message to {@link #source}.
   *
   * @param source is the source part of the message.
   * @return this instance of {@link MessageWorker}.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   */
  public MessageWorker setSource(String source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    this.source = source;
    return this;
  }

  /**
   * Sets the source part of the message to {@link #source}.
   *
   * @param prefix is the prefix of the source part of the message.
   * @param id is the ID of the source part of the message.
   * @return this instance of {@link MessageWorker}.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   */
  public MessageWorker setSource(String prefix, long id) {
    if (prefix == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }

    this.source = address(prefix, id);
    return this;
  }

  /**
   * Sets the command part of the message to {@link #command}.
   *
   * @param command is the command part of the message.
   * @return this instance of {@link MessageWorker}.
   * @throws IllegalArgumentException if {@code command} is {@code null}.
   */
  public MessageWorker setCommand(String command) {
    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null.");
    }
    this.command = command;
    return this;
  }

  /**
   * Builds the full message from the parts {@link #destination}, {@link #source}, and
   * {@link #command}, and sets it to {@link #message}.
   * If the message is complete, it will be built in the correct order,
   * and end with the indication that the message has ended ('=').
   * If either of the parts are {@code null}, only a partial message will be built.
   * End of transmission ('=') will not be added to the end of the message.
   *
   * @return this instance of {@link MessageWorker}.
   */
  public MessageWorker buildMessage() {
    StringBuilder sb = new StringBuilder();
    if (this.destination != null) {
      sb.append(this.destination);
    }
    sb.append("/");
    if (this.source != null) {
      sb.append(this.source);
    }
    sb.append("/");
    if (this.command != null) {
      sb.append(this.command);
    }
    sb.append("/");
    if (this.destination != null && this.source != null && this.command != null) {
      sb.append("=");
    }
    this.message = sb.toString();
    return this;
  }

  /**
   * Interprets the message into its specific parts, which are set to {@link #destination},
   * {@link #source}, and {@link #command}.
   */
  private void interpretMessage() {
    String[] messageParts = this.message.split("/");
    this.destination = messageParts[0];
    this.source = messageParts[1];
    this.command = messageParts[2];
  }

  /**
   * Returns the destination part of the message.
   *
   * @return the destination part of the message.
   */
  public String getDestination() {
    return this.destination;
  }

  /**
   * Returns the prefix of the destination part of the message.
   *
   * @return the prefix of the destination part of the message.
   */
  public String getDestinationPrefix() {
    if (this.destination == null || this.destination.length() < 2) {
      return null;
    }
    return this.destination.substring(0, 2);
  }

  /**
   * Returns the ID of the destination part of the message as a {@link Long}.
   *
   * @return the ID of the destination part of the message.
   */
  public Long getDestinationId() {
    if (this.destination == null || this.destination.length() < 3) {
      return null;
    }
    try {
      return Long.parseLong(this.destination.substring(2));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Returns the source part of the message.
   *
   * @return the source part of the message.
   */
  public String getSource() {
    return this.source;
  }

  /**
   * Returns the prefix of the source part of the message.
   *
   * @return the prefix of the source part of the message.
   */
  public String getSourcePrefix() {
    if (this.source == null || this.source.length() < 2) {
      return null;
    }
    return this.source.substring(0, 2);
  }

  /**
   * Returns the ID of the source part of the message as a {@link Long}.
   *
   * @return the ID of the source part of the message.
   */
  public Long getSourceId() {
    if (this.source == null || this.source.length() < 3) {
      return null;
    }
    try {
      return Long.parseLong(this.source.substring(2));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Returns the command part of the message.
   *
   * @return the command part of the message.
   */
  public String getCommand() {
    return this.command;
  }

  /**
   * Returns the key of the command part: the text before the first {@code :}, or the whole
   * command if it has no {@code :}. For {@code value:75} the key is {@code value}; for
   * {@code GET-COMMANDS} it is {@code GET-COMMANDS}.
   *
   * @return the trimmed key, or {@code null} if no command is set.
   */
  public String getCommandKey() {
    if (this.command == null) {
      return null;
    }
    int separator = this.command.indexOf(Protocol.SEPARATOR);
    return (separator < 0 ? this.command : this.command.substring(0, separator)).trim();
  }

  /**
   * Returns the value of the command part: the text after the first {@code :}. For
   * {@code value:75} the value is {@code 75}.
   *
   * @return the trimmed value, or an empty string if the command has no value.
   */
  public String getCommandValue() {
    if (this.command == null) {
      return "";
    }
    int separator = this.command.indexOf(Protocol.SEPARATOR);
    return separator < 0 ? "" : this.command.substring(separator + 1).trim();
  }

  /**
   * Returns the protocol message the command's key stands for.
   *
   * @return the {@link Protocol} constant, or empty if the key is not part of the protocol, for
   *        example because it is a device-specific command such as {@code power}.
   */
  public Optional<Protocol> getProtocol() {
    return Protocol.fromKey(getCommandKey());
  }

  /**
   * Returns the full message.
   *
   * @return the full message.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Checks if the message is complete, which means that all parts are set,
   * and the message ends with the indication that the message has ended ('=').
   *
   * @return {@code true} if the message is complete, otherwise {@code false}.
   */
  public boolean isMessageComplete() {
    return this.destination != null
        && this.source != null
        && this.command != null
        && this.message.endsWith("=");
  }
}
