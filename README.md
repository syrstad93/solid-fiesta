# Smart Home Automation
`IDATA2304` computer communication project.

## Group 1 members
- [Kristoffer Syrstad](https://github.com/syrstad93)
- [Iver Saunes Ronald](https://github.com/Iver909)
- [Matias Huse Heggdal](https://github.com/matiashh95)


## Requirements

This project uses ***Java*** with the following dependencies:

| Package           | Version           |
|---        |---        |
| Java      | 25        |
| Javafx    | 25.0.1    |
| Maven     | 25        |
| Junit     | 5.10.2    |



## How to run the application

**Compiling the project:**
````bash
mvn run compile
````

**Running the application:**

The _server_ can be started from: [Main.java](src/main/java/edu/ntnu/bidata/group1/Main.java) or started in demo mode (with example devices and room) from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

Running the control panel also from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

## Protocol

The [PROTOCOL.md](PROTOCOL.md) describes how server and client communicates with each other and general description about the project.
- Transport protocol, address, port etc.
- Format of messages (also see [protocol.java](src/main/java/edu/ntnu/bidata/group1/utility/Protocol.java)).
- Types of controllers, sensors and actuators.
- Automations
- Rooms.

## Sprints
Sprints are found under [sprints](sprints/)

