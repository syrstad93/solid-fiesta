# Smart home protocol

This document describes every message that the server and its clients (actuators, sensors and controllers) exchange. The message names are defined in code in [`Protocol.java`](src/main/java/edu/ntnu/bidata/group1/utility/Protocol.java). If you add or change a message, update both.

## Contents

1. [Transport and connections](#1-transport-and-connections)
2. [Message format](#2-message-format)
3. [Addresses](#3-addresses)
4. [The command part](#4-the-command-part)
5. [Messages to a device](#5-messages-to-a-device)
6. [Replies from a device](#6-replies-from-a-device)
7. [State and subscriptions](#7-state-and-subscriptions)
8. [Device-specific commands](#8-device-specific-commands)
9. [Sensors](#9-sensors)
10. [Messages to the server](#10-messages-to-the-server)
11. [Messages to a room](#11-messages-to-a-room)
12. [Automations](#12-automations)
13. [Replies and broadcasts from the server](#13-replies-and-broadcasts-from-the-server)
14. [Errors](#14-errors)
15. [Controller roles](#15-controller-roles)
16. [Example exchanges](#16-example-exchanges)
17. [Rules and limitations](#17-rules-and-limitations)

---

## 1. Transport and connections

| | |
|---|---|
| Protocol | TCP |
| Address | `127.0.0.1` (`UniversallyAvailableInformation.LOCALHOST`) |
| Port | `1238` (`UniversallyAvailableInformation.PORT_NUMBER`) |
| Framing | One message per line, UTF-8, ending in a line break |
| Topology | Star: every client connects to the server, and the server forwards messages between clients |

### Connecting: `HELLO` and `WELCOME`

The first message a client sends is an introduction, and the server answers with the client's id:

```
client → server   SE000/CH000/HELLO:kitchen-light,ACTUATOR,Light/=
server → client   CH001/SE000/WELCOME:1/=
```

| Part of `HELLO` | Meaning |
|---|---|
| `<name>` | The client's name, for example `kitchen-light`. It must be unique, must not be empty, and must not contain `/ = : ,`. |
| `<type>` | `ACTUATOR`, `SENSOR` or `CONTROLLER`. |
| `<kind>` | Optional, only for an actuator or sensor: what exactly the device is, as the name of its class in UpperCamelCase, for example `Light`, `Fan`, `DoorLock` or `TemperatureSensor` (`Device.getKind()`). The server remembers it, also while the device is offline, and passes it on in `DEVICE-ADDED`, so controllers know what a device is before it has sent any state. Must match `DeviceType.isValidKind`: names in capitals only, such as `OWNER`, are refused. |
| `<role>` | Only for a controller, in place of the kind: `OWNER`, `USER` or `GUEST`, as in `HELLO:anna-phone,CONTROLLER,OWNER`. The server records it, but does not check it yet (section 15). |

The id is the client's address (`CH<id>`). Every new connection is given the next free id (1, 2, 3, …), and a new name keeps that id. **A name that has been used before gets its old id back instead**, so a client that reconnects keeps its registration, its rooms, its automations and its subscribers.

If an older connection with the same name is still open, for example one that died without the server noticing yet, the server sends it `REPLACED` and closes it: the newest connection wins. A device that receives `REPLACED` does not reconnect, so two programs that accidentally use the same name do not keep pushing each other out.

A client that never sends `HELLO` still works, but keeps the id it got when it connected, and loses everything when it disconnects (see below).

### Heartbeat

| Side | What it does |
|---|---|
| Server | Sends `PING` to every client every **5 s**. Closes the connection to any client that has sent nothing for **15 s**. |
| Client | Answers every `PING` with `PONG`. Treats the connection as lost if nothing at all has arrived from the server for **15 s**, and reconnects. |

This finds connections that died without either side noticing, for example after a network cable was pulled or a laptop went to sleep. A dead client is dropped within about 15 to 20 seconds.

### Disconnecting

When a client's connection is lost, what the server does depends on whether the client introduced itself:

| | Client with a name | Client without a name |
|---|---|---|
| Registration, rooms, automations | Kept | Removed, as with `DEVICE-REMOVE` (section 10) |
| Subscriptions to it | Kept | Removed |
| Subscriptions it holds, its stored state | Removed; it subscribes and sends its state again when it reconnects | Removed |
| Broadcast | `DEVICE-OFFLINE:<id>` if it is a registered device, and `DEVICE-ONLINE:<id>` when it reconnects | `ROOM-REMOVED:<room>,<id>` for each room it was in; if it was a registered device, also `AUTOMATION-REMOVED:<id>` for each of its automations, then `DEVICE-REMOVED:<id>` |

## 2. Message format

Every message has four parts separated by `/`:

```
DESTINATION/SOURCE/COMMAND/=
```

| Part | Meaning |
|---|---|
| `DESTINATION` | Who the message is for. See [Addresses](#3-addresses). |
| `SOURCE` | Who sent it. **The server overwrites this** with the sender's real address (`CH<id>`), so a client can write anything here, for example `CH000`. |
| `COMMAND` | What to do, as `key` or `key:value`. See [The command part](#4-the-command-part). |
| `=` | End of message. |

Rules that every message must follow:

- It contains **exactly three** `/` characters, so the command part can never contain `/` (checked by `MessageWorker`).
- It ends with `=` (checked by `MessageWorker`).
- It contains no line break, because a line break ends the message (checked by `ClientConnectionHolder` before sending).

Example: client 3 asks client 1 to set its brightness to 75.

```
CH001/CH000/value:75/=   (sent by client 3)
CH001/CH003/value:75/=   (what client 1 receives: the server filled in the source)
```

## 3. Addresses

| Address | Meaning | Constant |
|---|---|---|
| `CH<id>` | One client, for example `CH001` or `CH012`. | - |
| `SE000` | The server itself. | `MessageWorker.SERVER` |
| `RO<id>` | A room, for example `RO010`. The room must already exist. | - |
| `AL000` | Every connected client, including the sender. | `MessageWorker.BROADCAST` |

Ids in an address are written with **three digits**, padded with leading zeros: client 1 is `CH001`, room 10 is `RO010`, and the server and broadcast use the placeholder `000`. Ids of 1000 and more are written in full (`CH1234`). Build addresses with `MessageWorker.address(prefix, id)`.

When reading a message, any number of digits is accepted, so `CH1`, `CH01` and `CH001` all mean client 1. Ids **inside** a command are plain numbers, not addresses, so they are not padded: `ROOM-ADD:10,1` puts client 1 in room 10.

## 4. The command part

The command part is either a **key** (`DEVICE-LIST`) or a **key and a value** separated by the first `:` (`value:75`). Everything after the first `:` belongs to the value, so the value may itself contain `:` (see `ROOM-BROADCAST:power:false` below).

There are two kinds of keys:

- **Protocol messages**: the fixed set in sections 5-13, defined in the `Protocol` enum. Every client and the server understand them.
- **Device-specific commands**: names such as `power` or `value`, chosen by each actuator (section 8). A device command may not use a protocol key.

### Naming rule

| Kind of name | Style | Examples |
|---|---|---|
| Protocol message | Upper case, words separated by hyphens | `DEVICE-ADD`, `ROOM-ADDED`, `GET-COMMANDS` |
| Device command or property | Lower case (camelCase for several words) | `power`, `value`, `powerOn` |

The two styles never look alike, so a key on the wire shows at once whether it is a protocol message or a device command. Further conventions for protocol messages:

- A message about one kind of thing starts with its name: `DEVICE-…`, `ROOM-…`.
- A query that returns many items ends in `-LIST`: `DEVICE-LIST`, `ROOM-LIST`, `ROOM-CLIENT-LIST`.
- A reply that confirms a change uses the past tense of the request: `ROOM-ADD` → `ROOM-ADDED`, `DEVICE-REMOVE` → `DEVICE-REMOVED`. (`ROOM-JOIN` and `ROOM-LEAVE` are confirmed with the same `ROOM-ADDED` and `ROOM-REMOVED`, because the result is the same.)
- A request to a device for information starts with `GET-`: `GET-COMMANDS`.

A device command may not reuse a protocol key in **any** letter case, so `error` and `Error` are both refused because of `ERROR`.

### Requests and replies

Every protocol message is either a **request**, which asks the receiver to do something, or a **reply**, which answers or announces something. **Replies are never answered**, not even with an error. That rule keeps two clients from sending errors back and forth forever.

## 5. Messages to a device

Sent to `CH<id>`, usually by a controller. Actuators and sensors both understand these.

| Constant | Format | Kind | What the device does |
|---|---|---|---|
| `COMMANDS_REQUEST` | `GET-COMMANDS` | request | Sends the sender one `COMMAND:<spec>` per command, then `COMMAND-END:<count>`. A sensor has no commands and only sends `COMMAND-END:0`. |
| `PING` | `PING` | request | Answers `PONG`. The server sends it every 5 s (section 1). |
| *(device command)* | `<name>:<argument>` | - | See [Device-specific commands](#8-device-specific-commands). |

A device's state is not asked for from the device itself: it goes through the server, to the clients that have subscribed to the device (section 7).

## 6. Replies from a device

| Constant | Format | Kind | Meaning |
|---|---|---|---|
| `COMMAND` | `COMMAND:<spec>` | reply | One command the device accepts. `<spec>` is described in section 8. |
| `COMMAND_END` | `COMMAND-END:<count>` | reply | All `<count>` commands have been sent. |
| `ERROR` | `ERROR:<command>,<reason>` | reply | The device rejected a command, for example `ERROR:value,value must be between 0 and 100`. |

When an actuator starts, it broadcasts its commands to `AL000` without being asked.

After carrying out a valid command, an actuator does not reply to the sender. Instead it sends every property that changed to the server, which passes it on to all subscribers (section 7), so the sender sees the result like every other controller.

## 7. State and subscriptions

A device's **state** is its properties, such as `type`, `powerOn` or `temperature`. Devices never send their state to other clients themselves. They send it to the server, and the server sends it on to every client that has **subscribed** to the device:

```
device  --STATE:powerOn,true-->  server  --powerOn:true-->  every subscriber
```

Commands still go straight to a device's address (`CH<id>`). Only state goes through the subscription list.

### From the device to the server

A device sends `STATE:<property>,<value>` to `SE000`, one message per property:

- its **whole state** once it is ready, and again after every reconnect, and
- **one property whenever it changes**: an actuator after a command, for each property the command changed, and a sensor when a measurement differs from the last one.

Nothing is sent while nothing changes. The server stores the latest value of every property of every device, so it does not need to ask again.

### From the server to the subscribers

For each `STATE` message, the server:

1. stores the value,
2. sends `CH<subscriber>/CH<device>/<property>:<value>/=` to every subscriber of the device. The **device is the source**, so a subscriber sees which device the state belongs to, and
3. if the value is a number, checks it against the [automations](#12-automations).

### Subscribing

| Constant | Format | Sent to | What the server does |
|---|---|---|---|
| `SUBSCRIBE` | `SUBSCRIBE:<deviceId>` | `SE000` | Subscribes the sender to a registered device, and at once sends it the device's stored state, one `<property>:<value>` message per property. Subscribing again just sends the state again. |
| `UNSUBSCRIBE` | `UNSUBSCRIBE:<deviceId>` | `SE000` | Stops sending the device's state to the sender. |

A `Controller` subscribes by itself to every registered device it learns about, and subscribes again after a reconnect. It can also call `unsubscribe(deviceId)` and `subscribe(deviceId)`.

A subscription ends when:

- the subscriber sends `UNSUBSCRIBE`,
- the device is unregistered, with `DEVICE-REMOVE`, or because it disconnected without having introduced itself with `HELLO`, or
- the subscriber disconnects.

A device that introduced itself keeps its subscribers when its connection is lost: they get its state again once it reconnects and sends it.

## 8. Device-specific commands

Each actuator declares the commands it accepts as a `CommandSpec`. A spec has a name, a parameter type, and a range or list of options. The device announces each one as `COMMAND:<spec>`, using this format:

| Parameter type | Spec format | Example spec | Example command |
|---|---|---|---|
| `NONE` | `<name>,NONE` | `toggle,NONE` | `toggle` |
| `BOOLEAN` | `<name>,BOOLEAN` | `power,BOOLEAN` | `power:true` |
| `INTEGER` | `<name>,INTEGER,<min>,<max>` | `value,INTEGER,0,100` | `value:75` |
| `DECIMAL` | `<name>,DECIMAL,<min>,<max>` | `temperature,DECIMAL,5.0,30.0` | `temperature:21.5` |
| `ENUM` | `<name>,ENUM,<option>\|<option>\|…` | `mode,ENUM,eco\|normal\|boost` | `mode:eco` |

Ranges include both ends. Names and options may not contain `/ = : , |`, and a name may not be a protocol key (for example `error` or `hello`, in any letter case).

The controller checks every command against the spec **before sending it**, and the device checks it again when it arrives. A valid command is executed, and the device sends what changed to the server (section 7). An invalid one is answered with `ERROR:<command>,<reason>`.

**Actuators in the project:**

| Class | Command | Spec | Effect |
|---|---|---|---|
| `Light` | `power:<true\|false>` | `power,BOOLEAN` | Turns the light on or off. |
| | `value:<0-100>` | `value,INTEGER,0,100` | Sets the brightness in percent. A value above 0 also turns the light on. |
| `Fan` | `power:<true\|false>` | `power,BOOLEAN` | Turns the fan on or off. |
| | `speed:<low\|medium\|high>` | `speed,ENUM,low\|medium\|high` | Sets the speed. Also turns the fan on. |
| `Heater` | `power:<true\|false>` | `power,BOOLEAN` | Turns the heater on or off. |
| | `target:<5.0-30.0>` | `target,DECIMAL,5.0,30.0` | Sets the target temperature in °C. Does not turn the heater on or off. |
| `DoorLock` | `lock` | `lock,NONE` | Locks the door. |
| | `unlock` | `unlock,NONE` | Unlocks the door. |

| Class | Properties reported | Starts as |
|---|---|---|
| `Light` | `type`, `powerOn`, `value` | off, brightness 0 |
| `Fan` | `type`, `powerOn`, `speed` | off, speed `low` |
| `Heater` | `type`, `powerOn`, `targetTemperature` | off, target 21.0 °C |
| `DoorLock` | `type`, `locked` | locked |

The heater is simulated: turning it on does not change what a `TemperatureSensor` measures.

A command without a parameter is sent as its name alone, for example `CH006/CH005/unlock/=`.

## 9. Sensors

A sensor measures something every 2 seconds. Each measurement is stored as a property, for example `temperature`, and if it differs from the last one, it is sent to the server as `STATE:temperature,21.5` (section 7). The server passes it on to the sensor's subscribers and checks it against the [automations](#12-automations).

Values are rounded to one decimal. Reading names should be lower case (the [naming rule](#naming-rule) for device properties), may not contain `/ = : ,`, and may not be a protocol key in any letter case.

A sensor accepts **no commands**: it answers `GET-COMMANDS` with `COMMAND-END:0` and any device command with `ERROR:<command>,a sensor accepts no commands`. A controller can tell a sensor from an actuator by its `type:SENSOR` property.

**Sensors in the project:**

| Class | Reading | Unit | Range | Start | Change per measurement |
|---|---|---|---|---|---|
| `TemperatureSensor` | `temperature` | °C | -10 to 40 | 21 | simulated: drifts up to ±0.3 |
| `HumiditySensor` | `humidity` | % | 0 to 100 | 45 | simulated: drifts up to ±1 |
| `Switch` | `on` | - | `0` (off) or `1` (on) | 0 | only when switched |

For demos and tests, `setTemperature(…)` and `setHumidity(…)` jump straight to a value and report it at once. A `Switch` is switched in code with `turnOn()`, `turnOff()` and `toggle()`, standing in for someone pressing it, and reports each change at once.

Because a switch reports a number, it drives automations like any other sensor. Use a threshold between 0 and 1: `on ABOVE 0.5` fires when the switch is turned on, and `on BELOW 0.5` fires when it is turned off. For example, `AUTOMATION-ADD:5,on,ABOVE,0.5,1,power:true` and `AUTOMATION-ADD:5,on,BELOW,0.5,1,power:false` make switch 5 control light 1.

## 10. Messages to the server

Sent to `SE000`. A request the server cannot carry out is answered with `ERROR:<request>,<reason>` (section 14).

| Constant | Format | Kind | What the server does | Answer |
|---|---|---|---|---|
| `HELLO` | `HELLO:<name>,<type>[,<kind>]` or `HELLO:<name>,CONTROLLER,<role>` | request | Records who the client is, and gives it its id: the same id as before if the name has been used before (section 1). | `WELCOME:<id>` to the sender |
| `PING` | `PING` | request | Nothing. | `PONG` to the sender |
| `STATE` | `STATE:<property>,<value>` | request | Stores the device's value, sends it to the device's subscribers, and, if it is a number, fires every automation whose condition it makes true (sections 7 and 12). Sent by devices. | `<property>:<value>` to every subscriber |
| `SUBSCRIBE` | `SUBSCRIBE:<deviceId>` | request | Subscribes the sender to a registered device (section 7). | The device's stored state, to the sender |
| `UNSUBSCRIBE` | `UNSUBSCRIBE:<deviceId>` | request | Unsubscribes the sender from the device. | - |
| `DEVICE_ADD` | `DEVICE-ADD:<id>` | request | Registers connected client `<id>` as a device in the home. Fails if the client is not connected, is already registered, or is the sender itself. | `DEVICE-ADDED:<id>,<name>,ONLINE[,<kind>]` to **everyone** |
| `DEVICE_REMOVE` | `DEVICE-REMOVE:<id>` | request | Unregisters the device, takes it out of every room, removes every subscription to it, and removes every automation that uses it. | `ROOM-REMOVED:<room>,<id>` for each room, `AUTOMATION-REMOVED:<id>` for each automation, then `DEVICE-REMOVED:<id>`, to **everyone** |
| `DEVICE_LIST` | `DEVICE-LIST` | request | - | One `DEVICE-ADDED:<id>,<name>,<ONLINE\|OFFLINE>[,<kind>]` per registered device, to the sender |
| `ROOM_CREATE` | `ROOM-CREATE:<name>` | request | Creates an empty room called `<name>`. The **server chooses the room's id**. The name is trimmed and must not be blank or contain `/`, `=` or line breaks. | `ROOM-CREATED:<room>,<name>` to **everyone** |
| `ROOM_RENAME` | `ROOM-RENAME:<room>,<name>` | request | Sets or changes the name of an existing room. Everything after the first comma is the name, with the same rules as `ROOM-CREATE`. | `ROOM-RENAMED:<room>,<name>` to **everyone** |
| `ROOM_ADD` | `ROOM-ADD:<room>,<id>` | request | Puts registered device `<id>` in room `<room>`, creating the room (without a name) if needed. | `ROOM-ADDED:<room>,<id>` to **everyone** |
| `ROOM_REMOVE` | `ROOM-REMOVE:<room>,<id>` | request | Takes client `<id>` out of the room. A room that becomes empty is deleted, unless it has a name. | `ROOM-REMOVED:<room>,<id>` to **everyone** |
| `ROOM_DELETE` | `ROOM-DELETE:<room>` | request | Deletes the room, named or not, even if devices are in it. The devices stay registered; they are only no longer in the room. Fails if the room does not exist. | `ROOM-DELETED:<room>` to **everyone** |
| `ROOM_LIST` | `ROOM-LIST` | request | - | One `ROOM-CREATED:<room>,<name>` per named room, then one `ROOM-ADDED:<room>,<id>` per client in each room, to the sender |
| `AUTOMATION_ADD` | `AUTOMATION-ADD:<sensor>,<reading>,<ABOVE\|BELOW>,<threshold>,<device>,<command>` | request | Adds an automation (section 12). Both the sensor and the device must be registered. | `AUTOMATION-ADDED:<id>,…` to **everyone** |
| `AUTOMATION_REMOVE` | `AUTOMATION-REMOVE:<id>` | request | Removes an automation. | `AUTOMATION-REMOVED:<id>` to **everyone** |
| `AUTOMATION_LIST` | `AUTOMATION-LIST` | request | - | One `AUTOMATION-ADDED:<id>,…` per automation, to the sender |

## 11. Messages to a room

Sent to `RO<id>`. The room must already exist (create it with `ROOM-CREATE` or `ROOM-ADD`), otherwise the server answers with an error.

| Constant | Format | Kind | What happens | Answer |
|---|---|---|---|---|
| `JOIN_ROOM` | `ROOM-JOIN` | request | The **sender** joins the room. | `ROOM-ADDED:<room>,<sender>` to **everyone** |
| `LEAVE_ROOM` | `ROOM-LEAVE` | request | The **sender** leaves the room. A room that becomes empty is deleted. | `ROOM-REMOVED:<room>,<sender>` to **everyone** |
| `GET_ROOM_CLIENTS` | `ROOM-CLIENT-LIST` | request | - | `ROOM-CLIENTS:<id>,<id>,…` to the sender |
| `ROOM_BROADCAST` | `ROOM-BROADCAST:<command>` | request | Forwards `<command>` to every client in the room. The source stays the original sender, so devices reply to that sender. | - |

Example: `RO010/CH000/ROOM-BROADCAST:power:false/=` turns off every light in room 10.

## 12. Automations

An automation is a rule the **server** follows on its own: *when a sensor's reading goes above or below a threshold, send a command to a device.* Automations keep working when no controller is connected.

The server checks every `STATE` value that is a number (section 7), so the watched device is usually a sensor, but can be any registered device. For example, "when the light's `value` goes above 50, run the fan at `high`".

### Format

```
AUTOMATION-ADD:<sensor>,<reading>,<condition>,<threshold>,<device>,<command>
```

| Part | Meaning | Example |
|---|---|---|
| `<sensor>` | Client id of the device to watch, usually a sensor | `2` |
| `<reading>` | Which property of that device; its value must be a number | `temperature` |
| `<condition>` | `ABOVE` (reading > threshold) or `BELOW` (reading < threshold) | `ABOVE` |
| `<threshold>` | A number | `25` |
| `<device>` | Client id of the device to control | `1` |
| `<command>` | The device command to send, with its argument. Comes last, so it may contain `:`. | `power:true` |

Example: `AUTOMATION-ADD:2,temperature,ABOVE,25,1,power:true` means "when the temperature on sensor 2 goes above 25, turn on device 1".

The server gives each automation an id (1, 2, 3, …) and announces it to everyone as `AUTOMATION-ADDED:<id>,<sensor>,<reading>,<condition>,<threshold>,<device>,<command>`.

### When an automation fires

An automation fires **once when its condition becomes true**, not on every reading while it stays true. When a reading no longer meets the condition, the automation is ready to fire again.

| Reading (rule: `ABOVE 25`) | 24 | 26 | 27 | 24 | 26 |
|---|---|---|---|---|---|
| Fires? | no | **yes** | no (still above) | no (resets) | **yes** |

If the sensor's latest reading already meets the condition when the automation is added, it fires straight away.

When an automation fires, the server:

1. sends the command to the device **from the server** (`CH<device>/SE000/<command>/=`). The device carries it out and sends what changed to the server, which passes it on to its subscribers, so controllers see the change.
2. broadcasts `AUTOMATION-TRIGGERED:<id>,<value>` to everyone, with the reading that made it fire.

To switch something back, add a second automation with the opposite condition, for example `BELOW 23 → power:false`. Using a slightly different threshold (23 instead of 25) stops the device from switching back and forth when the reading hovers around one value.

### Checks

- The **controller** checks the command against the device's announced commands (type and range) before sending `AUTOMATION-ADD`, and checks that the watched device reports that property as a number, once it knows its state.
- The **server** checks the format, that the sensor and the device are registered, and that the command is a device command and not a protocol message.
- When a device is unregistered with `DEVICE-REMOVE`, every automation that uses it is removed too.

## 13. Replies and broadcasts from the server

These are all replies, so a receiver never answers them. The one exception is `PING`, a request the server sends to check that a client is still there, which every client answers with `PONG`. Messages marked **everyone** are sent to `AL000`, so every client, whatever its role, sees the same devices and rooms.

| Constant | Format | Sent to | Meaning |
|---|---|---|---|
| `WELCOME` | `WELCOME:<id>` | sender | Answer to `HELLO`: the client's id (section 1). |
| `REPLACED` | `REPLACED` | the old connection | A newer connection introduced itself with the same name; this one is closed. The client must not reconnect. |
| `PING` | `PING` | every client, every 5 s | A request, not a reply: answer with `PONG` (section 1). |
| `PONG` | `PONG` | sender | Answer to `PING`. |
| `DEVICE_ADDED` | `DEVICE-ADDED:<id>,<name>,<ONLINE\|OFFLINE>[,<kind>]` | everyone, or the sender after `DEVICE-LIST` | Client `<id>`, called `<name>`, is a registered device, and is connected or not. `<kind>`, for example `Fan`, is what the device said it is in `HELLO`; left out if it gave none. |
| `DEVICE_ONLINE` | `DEVICE-ONLINE:<id>` | everyone | A registered device connected again. |
| `DEVICE_OFFLINE` | `DEVICE-OFFLINE:<id>` | everyone | A registered device lost its connection. It stays registered. |
| `DEVICE_REMOVED` | `DEVICE-REMOVED:<id>` | everyone | Device `<id>` was unregistered. |
| `ROOM_CREATED` | `ROOM-CREATED:<room>,<name>` | everyone, or the sender after `ROOM-LIST` | Room `<room>` exists and is called `<name>`. |
| `ROOM_RENAMED` | `ROOM-RENAMED:<room>,<name>` | everyone | Room `<room>` is now called `<name>`. |
| `ROOM_ADDED` | `ROOM-ADDED:<room>,<id>` | everyone, or the sender after `ROOM-LIST` | Client `<id>` is in room `<room>`. |
| `ROOM_REMOVED` | `ROOM-REMOVED:<room>,<id>` | everyone | Client `<id>` left room `<room>`. |
| `ROOM_DELETED` | `ROOM-DELETED:<room>` | everyone | Room `<room>` was deleted, with its name and everything in it. No `ROOM-REMOVED` is sent for the clients that were in it. |
| `ROOM_CLIENTS` | `ROOM-CLIENTS:<id>,<id>,…` | sender | The clients in a room; the value is empty for an empty room. Comes from `RO<room>`. |
| *(state)* | `<property>:<value>`, from `CH<device>` | subscribers | One property of a device's state, for example `CH005/CH001/powerOn:true/=` (section 7). |
| `AUTOMATION_ADDED` | `AUTOMATION-ADDED:<id>,<sensor>,<reading>,<condition>,<threshold>,<device>,<command>` | everyone, or the sender after `AUTOMATION-LIST` | An automation exists. |
| `AUTOMATION_REMOVED` | `AUTOMATION-REMOVED:<id>` | everyone | An automation was removed. |
| `AUTOMATION_TRIGGERED` | `AUTOMATION-TRIGGERED:<id>,<value>` | everyone | An automation fired because its sensor reported `<value>`. |
| `ERROR` | `ERROR:<request>,<reason>` | sender | See below. |

## 14. Errors

When the server cannot handle a message, it replies to the sender with:

```
CH<sender>/SE000/ERROR:<request>,<reason>/=
```

The connection stays open. Typical reasons:

- the message is malformed (wrong number of `/`, missing `=`)
- the command is unknown to the server or the room
- the destination client or room does not exist
- a device is not connected, is already registered, or is not in the room

`<request>` is the key of the rejected message, for example `ROOM-ADD`, or `?` if the message had none. Because the server handles a client's messages in the order they were sent, a client can match the error to the oldest request of that kind it is still waiting for; `Controller` does this to fail the right future.

A device that rejects a command replies the same way, but from its own address, with the command's name as the key: `CH<sender>/CH<device>/ERROR:value,value must be between 0 and 100/=`.

Any `/` or line break in a reason is replaced with a space, so the reply is always a valid message.

## 15. Controller roles

A `Controller` gets a `Role` when it is created. The role cannot be changed afterwards.

| What | Messages | `GUEST` | `USER` | `OWNER` |
|---|---|:-:|:-:|:-:|
| See devices, their commands and state, rooms and automations | `DEVICE-LIST`, `ROOM-LIST`, `AUTOMATION-LIST`, `GET-COMMANDS`, `SUBSCRIBE`, `UNSUBSCRIBE` | ✓ | ✓ | ✓ |
| Control devices | device commands such as `power:true` | ✓ | ✓ | ✓ |
| Create, rename and delete rooms, put devices in rooms and take them out | `ROOM-CREATE`, `ROOM-RENAME`, `ROOM-DELETE`, `ROOM-ADD`, `ROOM-REMOVE` | | ✓ | ✓ |
| Add and remove automations | `AUTOMATION-ADD`, `AUTOMATION-REMOVE` | | ✓ | ✓ |
| Add and remove devices | `DEVICE-ADD`, `DEVICE-REMOVE` | | | ✓ |

The role is checked **in the controller**, before a message is sent. A controller also gives its role in `HELLO` (section 1), so the server knows it, but the server does not check it yet (see section 17).

## 16. Example exchanges

In these examples, client 1 is a `Light`, client 3 is an owner controller, and client 5 is a guest controller. Messages are shown as they arrive, after the server has filled in the source.

### A controller starts

```
5 → server   SE000/CH000/HELLO:guest-phone,CONTROLLER,GUEST/=
server → 5   CH005/SE000/WELCOME:5/=
5 → server   SE000/CH005/STATE:type,CONTROLLER/=   (every client sends its state when it starts)
5 → server   SE000/CH005/DEVICE-LIST/=
5 → server   SE000/CH005/ROOM-LIST/=
5 → server   SE000/CH005/AUTOMATION-LIST/=
server → 5   CH005/SE000/DEVICE-ADDED:1,kitchen-light,ONLINE,Light/=   (one per registered device)
server → 5   CH005/SE000/ROOM-ADDED:10,1/=   (one per client in a room)
server → 5   CH005/SE000/AUTOMATION-ADDED:1,2,temperature,ABOVE,25.0,1,power:true/=   (one per automation)
5 → 1        CH001/CH005/GET-COMMANDS/=      (for each new device)
5 → server   SE000/CH005/SUBSCRIBE:1/=
1 → 5        CH005/CH001/COMMAND:power,BOOLEAN/=
1 → 5        CH005/CH001/COMMAND:value,INTEGER,0,100/=
1 → 5        CH005/CH001/COMMAND-END:2/=
server → 5   CH005/CH001/type:ACTUATOR/=     (the stored state, with the device as source)
server → 5   CH005/CH001/powerOn:false/=
server → 5   CH005/CH001/value:0/=
```

### The owner adds a device

```
3 → server   SE000/CH003/DEVICE-ADD:1/=
server → all AL000/SE000/DEVICE-ADDED:1,kitchen-light,ONLINE,Light/=
(every controller that did not know device 1 sends it GET-COMMANDS and the server SUBSCRIBE:1, as above)
```

### A guest sets the brightness

```
5 → 1        CH001/CH005/value:75/=
1 → server   SE000/CH001/STATE:powerOn,true/=   (only the properties that changed)
1 → server   SE000/CH001/STATE:value,75/=
server → 5   CH005/CH001/powerOn:true/=         (and to every other subscriber, such as 3)
server → 5   CH005/CH001/value:75/=
```

If the guest had sent `value:150`, the controller would have refused before sending. A device that receives an invalid value replies `ERROR:value,value must be between 0 and 100`.

### Putting a device in a room and switching the room off

```
3 → server   SE000/CH003/ROOM-ADD:10,1/=
server → all AL000/SE000/ROOM-ADDED:10,1/=
5 → room 10  RO010/CH005/ROOM-BROADCAST:power:false/=
room → 1     RO010/CH005/power:false/=   (also sent to every other client in room 10)
1 → server   SE000/CH001/STATE:powerOn,false/=
server → 5   CH005/CH001/powerOn:false/=   (and to every other subscriber)
```

### An automation turns on a light when it gets warm

Client 2 is a `TemperatureSensor`, and client 6 is a user controller. The server also passes each temperature on to the sensor's subscribers; those messages are left out here.

```
6 → server   SE000/CH006/AUTOMATION-ADD:2,temperature,ABOVE,25.0,1,power:true/=
server → all AL000/SE000/AUTOMATION-ADDED:1,2,temperature,ABOVE,25.0,1,power:true/=
2 → server   SE000/CH002/STATE:temperature,24.8/=   (below 25: nothing happens)
2 → server   SE000/CH002/STATE:temperature,25.1/=   (above 25: automation 1 fires)
server → 1   CH001/SE000/power:true/=
server → all AL000/SE000/AUTOMATION-TRIGGERED:1,25.1/=
1 → server   SE000/CH001/STATE:powerOn,true/=
server → 5   CH005/CH001/powerOn:true/=             (and to the other subscribers, 3 and 6)
2 → server   SE000/CH002/STATE:temperature,25.4/=   (still above: does not fire again)
```

## 17. Rules and limitations

- **State is only sent when it changes.** The server keeps the latest value of every property, so a new subscriber gets the current state at once, without the device being asked.
- **Replies are never answered.** A client that receives a reply it doesn't need ignores it silently. The server follows the same rule: an `ERROR` sent to the server is logged, not answered.
- **Unknown device commands are answered with an error**, but unknown *replies* are ignored.
- **Names must be unique.** Two clients with the same name get the same id, and the newer one pushes the older one out (section 1).
- **Names are only remembered while the server runs.** After a server restart, devices get new ids and must be added again.
- **Roles are not enforced by the server.** A client that does not use the `Controller` class can send `DEVICE-ADD` or `ROOM-ADD` directly. The server knows each controller's role from `HELLO`, but that role is whatever the client claims, so enforcing it would also need a way to prove it, for example a login step.
- **Automations, rooms and registered devices live in the server's memory.** They are lost when the server restarts.
- **`ROOM-JOIN` cannot create a room.** A client can only join a room that already exists; rooms are created with `ROOM-CREATE` or `ROOM-ADD`.
- **Named rooms are never deleted automatically.** There is no `ROOM-DELETE` yet, so a room created with `ROOM-CREATE` stays until the server restarts.
- **New messages follow the [naming rule](#naming-rule).** `ProtocolTest` fails if a key in `Protocol` breaks it.
