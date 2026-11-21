package edu.ntnu.bidata.group1.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Every fixed message the clients and the server understand.
 *
 * <p>The full description of the protocol, with message formats, who sends what, and example
 * exchanges, is in {@code PROTOCOL.md} in the project root. Keep the two in sync.
 *
 * <p>A message's command part is {@code key} or {@code key:value}. This enum lists the keys that
 * are part of the protocol itself. It does <em>not</em> list device-specific commands such as
 * {@code power} or {@code value}: each actuator declares those itself with a
 * {@code CommandSpec}, so that controllers can work with devices they have never seen before.
 *
 * <h2>Naming</h2>
 * Every protocol key is written in upper case, with words separated by hyphens:
 * {@code DEVICE-ADD}, {@code ROOM-ADDED}, {@code GET-COMMANDS}. Messages about one thing start with
 * its name ({@code DEVICE-}, {@code ROOM-}); a reply that confirms a request uses the past tense
 * of the request ({@code ROOM-ADD} -> {@code ROOM-ADDED}). Device commands and properties are
 * written in lower case ({@code power}, {@code powerOn}), so the two never look alike, and a
 * device command may not reuse a protocol key in any letter case (see
 * {@link #isReservedName(String)}).
 *
 * <p>Use {@link #withValue(Object)} to build a command, and
 * {@link MessageWorker#getProtocol()} or {@link #fromKey(String)} to recognise one:
 * <pre>{@code
 * sendMessage(MessageWorker.SERVER, Protocol.ROOM_ADD.withValue(roomId + "," + deviceId));
 *
 * switch (messageWorker.getProtocol().orElse(null)) { ... }
 * }</pre>
 */
public enum Protocol {

  // requests to a device

  /** {@code GET-COMMANDS} - send me your commands. */
  COMMANDS_REQUEST("GET-COMMANDS", Kind.REQUEST),


  // replies from a device

  /**
   * {@code COMMAND:<spec>} - one available command, for example
   * {@code COMMAND:value,INTEGER,0,100}.
   */
  COMMAND("COMMAND", Kind.REPLY),

  /** {@code COMMAND-END:<count>} - all available commands have been sent. */
  COMMAND_END("COMMAND-END", Kind.REPLY),

  // reply from a device or the server

  /**
   * {@code ERROR:<request>,<reason>} - a message from the receiver was rejected. {@code <request>}
   * is the key of the rejected message, for example {@code ROOM-ADD}, or {@code ?} if it had none.
   */
  ERROR("ERROR", Kind.REPLY),

  // requests to the server (SE000)

  /**
   * {@code HELLO:<name>,<type>} or {@code HELLO:<name>,CONTROLLER,<role>} - the first message a
   * client sends after connecting. Answered with {@link #WELCOME}.
   */
  HELLO("HELLO", Kind.REQUEST),

  /**
   * {@code PING} - are you still there? Answered with {@link #PONG}. The server sends it to every
   * client every few seconds; clients may also send it to the server.
   */
  PING("PING", Kind.REQUEST),

  /** {@code DEVICE-ADD:<id>} - register a connected client as a device. */
  DEVICE_ADD("DEVICE-ADD", Kind.REQUEST),

  /** {@code DEVICE-REMOVE:<id>} - unregister a device and take it out of every room. */
  DEVICE_REMOVE("DEVICE-REMOVE", Kind.REQUEST),

  /** {@code DEVICE-LIST} - send me every registered device. */
  DEVICE_LIST("DEVICE-LIST", Kind.REQUEST),

  /** {@code ROOM-CREATE:<name>} - create an empty, named room; the server chooses its id. */
  ROOM_CREATE("ROOM-CREATE", Kind.REQUEST),

  /** {@code ROOM-ADD:<roomId>,<id>} - put a registered device in a room. */
  ROOM_ADD("ROOM-ADD", Kind.REQUEST),

  /** {@code ROOM-RENAME:<roomId>,<name>} - set or change the name of a room. */
  ROOM_RENAME("ROOM-RENAME", Kind.REQUEST),

  /** {@code ROOM-REMOVE:<roomId>,<id>} - take a client out of a room. */
  ROOM_REMOVE("ROOM-REMOVE", Kind.REQUEST),

  /** {@code ROOM-LIST} - send me every room and the clients in it. */
  ROOM_LIST("ROOM-LIST", Kind.REQUEST),

  /**
   * {@code STATE:<property>,<value>} - a device's property has this value, for example
   * {@code STATE:powerOn,true}. The server stores it, sends it to the device's subscribers and,
   * if the value is a number, checks it against the automations.
   */
  STATE("STATE", Kind.REQUEST),

  /** {@code SUBSCRIBE:<deviceId>} - send me this device's state, now and whenever it changes. */
  SUBSCRIBE("SUBSCRIBE", Kind.REQUEST),

  /** {@code UNSUBSCRIBE:<deviceId>} - stop sending me this device's state. */
  UNSUBSCRIBE("UNSUBSCRIBE", Kind.REQUEST),

  /**
   * {@code AUTOMATION-ADD:<sensor>,<reading>,<ABOVE|BELOW>,<threshold>,<device>,<command>}
   * - add an automation rule.
   */
  AUTOMATION_ADD("AUTOMATION-ADD", Kind.REQUEST),

  /** {@code AUTOMATION-REMOVE:<automationId>} - remove an automation rule. */
  AUTOMATION_REMOVE("AUTOMATION-REMOVE", Kind.REQUEST),

  /** {@code AUTOMATION-LIST} - send me every automation rule. */
  AUTOMATION_LIST("AUTOMATION-LIST", Kind.REQUEST),

  // requests to a room (RO<id>)

  /** {@code ROOM-JOIN} - the sender joins the room it is sent to. */
  JOIN_ROOM("ROOM-JOIN", Kind.REQUEST),

  /** {@code ROOM-LEAVE} - the sender leaves the room it is sent to. */
  LEAVE_ROOM("ROOM-LEAVE", Kind.REQUEST),

  /** {@code ROOM-CLIENT-LIST} - send me the ids of the clients in the room. */
  GET_ROOM_CLIENTS("ROOM-CLIENT-LIST", Kind.REQUEST),

  /**
   * {@code ROOM-BROADCAST:<command>} - forward {@code <command>} to every client in the
   * room, for example {@code ROOM-BROADCAST:power:false}.
   */
  ROOM_BROADCAST("ROOM-BROADCAST", Kind.REQUEST),

  // replies from the server

  /** {@code PONG} - answer to {@link #PING}. */
  PONG("PONG", Kind.REPLY),

  /**
   * {@code WELCOME:<id>} - answer to {@link #HELLO}: the client's id. A client that reconnects
   * with the same name gets the same id back.
   */
  WELCOME("WELCOME", Kind.REPLY),

  /**
   * {@code REPLACED} - sent to a client's old connection just before the server closes it,
   * because a newer connection introduced itself with the same name. The client must not
   * reconnect.
   */
  REPLACED("REPLACED", Kind.REPLY),

  /**
   * {@code DEVICE-ADDED:<id>,<name>,<ONLINE|OFFLINE>} - broadcast: a device was registered, or,
   * after {@link #DEVICE_LIST}, a device is registered.
   */
  DEVICE_ADDED("DEVICE-ADDED", Kind.REPLY),

  /** {@code DEVICE-ONLINE:<id>} - broadcast: a registered device connected again. */
  DEVICE_ONLINE("DEVICE-ONLINE", Kind.REPLY),

  /** {@code DEVICE-OFFLINE:<id>} - broadcast: a registered device lost its connection. */
  DEVICE_OFFLINE("DEVICE-OFFLINE", Kind.REPLY),

  /** {@code DEVICE-REMOVED:<id>} - broadcast: a device was unregistered. */
  DEVICE_REMOVED("DEVICE-REMOVED", Kind.REPLY),

  /** {@code ROOM-CREATED:<roomId>,<name>} - broadcast: a named room was created. */
  ROOM_CREATED("ROOM-CREATED", Kind.REPLY),

  /** {@code ROOM-RENAMED:<roomId>,<name>} - broadcast: a room was renamed. */
  ROOM_RENAMED("ROOM-RENAMED", Kind.REPLY),

  /** {@code ROOM-ADDED:<roomId>,<id>} - broadcast: a client was put in a room. */
  ROOM_ADDED("ROOM-ADDED", Kind.REPLY),

  /** {@code ROOM-REMOVED:<roomId>,<id>} - broadcast: a client was taken out of a room. */
  ROOM_REMOVED("ROOM-REMOVED", Kind.REPLY),

  /** {@code ROOM-CLIENTS:<id>,<id>,...} - answer to {@link #GET_ROOM_CLIENTS}. */
  ROOM_CLIENTS("ROOM-CLIENTS", Kind.REPLY),

  /**
   * {@code AUTOMATION-ADDED:<automationId>,<sensor>,<reading>,<ABOVE|BELOW>,<threshold>,<device>,
   * <command>} - broadcast: an automation rule was added.
   */
  AUTOMATION_ADDED("AUTOMATION-ADDED", Kind.REPLY),

  /** {@code AUTOMATION-REMOVED:<automationId>} - broadcast: an automation was removed. */
  AUTOMATION_REMOVED("AUTOMATION-REMOVED", Kind.REPLY),

  /**
   * {@code AUTOMATION-TRIGGERED:<automationId>,<value>} - broadcast: an automation fired
   * because its sensor reported {@code <value>}.
   */
  AUTOMATION_TRIGGERED("AUTOMATION-TRIGGERED", Kind.REPLY);

  /** Whether a message asks for something, or reports or answers something. */
  public enum Kind {
    /** Asks the receiver to do something. */
    REQUEST,
    /** Answers a request or announces something. Never answered, so replies cannot loop. */
    REPLY
  }

  /** Separates the key from the value in a command, as in {@code key:value}. */
  public static final char SEPARATOR = ':';

  private static final Map<String, Protocol> BY_KEY = new HashMap<>();

  static {
    for (Protocol protocol : values()) {
      BY_KEY.put(protocol.key, protocol);
    }
  }

  private final String key;
  private final Kind kind;

  Protocol(String key, Kind kind) {
    this.key = key;
    this.kind = kind;
  }

  /**
   * Returns the key as it is written in a message.
   *
   * @return for example {@code ROOM-ADD} or {@code COMMAND}
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns whether this message is a request or a reply.
   *
   * @return the kind
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Returns whether this message is a reply or announcement, which must never be answered.
   *
   * @return {@code true} for replies
   */
  public boolean isReply() {
    return kind == Kind.REPLY;
  }

  /**
   * Builds the command part of a message with this key and the given value.
   *
   * @param value the value; written with {@link String#valueOf(Object)}
   * @return for example {@code ROOM-ADD:10,1}
   */
  public String withValue(Object value) {
    return key + SEPARATOR + value;
  }

  /**
   * Returns the command part of a message with this key and no value.
   *
   * @return the key, for example {@code DEVICE-LIST}
   */
  @Override
  public String toString() {
    return key;
  }

  /**
   * Builds the command part of an {@code ERROR} message. Characters that would break the message
   * format ({@code /} and line breaks) are replaced with spaces.
   *
   * @param requestKey the key of the rejected message, for example {@code ROOM-ADD}; {@code ?} is
   *     used if it is null or blank
   * @param reason why the message was rejected
   * @return for example {@code ERROR:ROOM-ADD,Room and client id must be numbers.}
   */
  public static String error(String requestKey, String reason) {
    String key = requestKey == null || requestKey.isBlank() ? "?" : requestKey.trim();
    return ERROR.withValue(clean(key).replace(',', ' ') + "," + clean(String.valueOf(reason)));
  }

  private static String clean(String text) {
    return text.replace('/', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  /**
   * Returns whether a device command may not use this name, because it is a protocol key in any
   * letter case. For example {@code error} and {@code Error} are reserved because of
   * {@code ERROR}.
   *
   * @param name the proposed device command name
   * @return {@code true} if the name is reserved
   */
  public static boolean isReservedName(String name) {
    if (name == null) {
      return false;
    }
    String trimmed = name.trim();
    for (Protocol protocol : values()) {
      if (protocol.key.equalsIgnoreCase(trimmed)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds the protocol message with the given key.
   *
   * @param key the key as written in a message, for example {@code ROOM-ADDED}
   * @return the protocol message, or empty if the key is not part of the protocol, for example
   *     because it is a device-specific command such as {@code power}
   */
  public static Optional<Protocol> fromKey(String key) {
    return key == null ? Optional.empty() : Optional.ofNullable(BY_KEY.get(key.trim()));
  }
}
