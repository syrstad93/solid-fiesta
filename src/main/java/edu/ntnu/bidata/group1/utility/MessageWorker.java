package edu.ntnu.bidata.group1.utility;

import edu.ntnu.bidata.group1.network.server.ClientHandler;
import edu.ntnu.bidata.group1.network.server.Server;

/**
 * Manages a transmission message,
 * and contains methods for setting, building, editing and returning the specific parts.
 *
 * <p>
 *   A command message is a segmented {@link String} with the below structure.
 *   The separation between each segment is indicated by a {@code /}.
 * </p>
 * <p>
 *   {@code Destination/Source/Command/End of transmission}
 * </p>
 * <p>
 *   {@code Destination} contains a prefix, followed by the ID of the recipient if relevant.
 *   These are the possible prefixes:
 * </p>
 * <ul>
 *   <li>
 *     {@code CH}: a {@link ClientHandler}. Is followed by the ID of the client handler.
 *   </li>
 *   <li>
 *     {@code SE}: the {@link Server}. Is followed by placeholder ID {@code 00}, or no ID at all.
 *   </li>
 *   <li>
 *     {@code RO}: a room of clients. Is followed by the ID of the room.
 *   </li>
 *   <li>
 *     {@code AL}: broadcast to all. Is followed by placeholder ID {@code 00}, or no ID at all.
 *   </li>
 * </ul>
 * <p>
 *   {@code Source} contains a prefix, followed by the ID of the sender if relevant.
 *   These are the possible prefixes:
 * </p>
 * <ul>
 *   <li>
 *     {@code CH}: a {@link ClientHandler}. Is followed by the ID of the client handler.
 *   </li>
 *   <li>
 *     {@code SE}: the {@link Server}. Is followed by placeholder ID {@code 00}, or no ID at all.
 *   </li>
 * </ul>
 * <p>
 *   {@code Command} is the command information for the destination device.
 *   This part of the message is specific for the device.
 * </p>
 * <p>
 *   {@code End of transmission} is the symbol {@code =}, which signifies the end of the message.
 * </p>
 */
public class MessageWorker {

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
  public MessageWorker()  {
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
    if(message.length() - message.replace("/", "").length() != 3) {
      throw new IllegalArgumentException(
          "Message must contain exactly three '/' characters to separate the parts."
      );
    }
    if (!message.endsWith("=")) {
      throw new IllegalArgumentException(
          "Indication that the message has ended must ended ('=') is missing"
      );
    }
    this.message = message;
    interpretMessage();
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

    if (id < 9) {
      this.destination = prefix + "0" + id;
    } else {
      this.destination = prefix + id;
    }

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

    if (id < 9) {
      this.source = prefix + "0" + id;
    } else {
      this.source = prefix + id;
    }

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
