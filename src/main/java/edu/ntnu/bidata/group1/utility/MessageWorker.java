package edu.ntnu.bidata.group1.utility;

import edu.ntnu.bidata.group1.network.server.ClientHandler;
import edu.ntnu.bidata.group1.network.server.Server;

/**
 * Interprets a transmission message, and contains methods for returning the specific parts.
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
public class MessageInterpreter {

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
  public MessageInterpreter(String message) {
    setMessage(message);
    interpretMessage();
  }

  /**
   * Sets the full message to {@link #message}.
   *
   * @param message is a command message.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   * @throws IllegalArgumentException if {@code message} does not end with the indication that the
   *        message has ended ('=').
   */
  private void setMessage(String message) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }
    if (!message.endsWith("=")) {
      throw new IllegalArgumentException(
          "Indication that the message has ended must ended ('=') is missing"
      );
    }
    this.message = message;
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
}
