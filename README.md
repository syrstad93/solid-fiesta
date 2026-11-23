# Smart Home Automation
`IDATA2304` computer communication project.

## Group 1 members
- [Kristoffer Syrstad](https://github.com/syrstad93)
- [Iver Saunes Roald](https://github.com/Iver909)
- [Matias Huse Heggdal](https://github.com/matiashh95)

## Project description

This is a project for the course `IDATA2304` at NTNU.
The project is a smart home automation system that allows users to control and monitor various
devices in their home through a graphical user interface (GUI).
The system supports different types of controllers, sensors, and actuators,
enabling users to create automations and manage their smart home environment effectively.

Essential features include:
- **Device Management**: Add, remove, and configure smart devices.
- **Room Management**: Organize devices into rooms for better control.
- **Automations**: Create rules and automations based on device states and sensor readings.
- **Real-time Monitoring**: Monitor device status and sensor data in real-time.
- **User-friendly GUI**: Intuitive interface for easy interaction with the system.
- **Roles and Permissions**: Different user roles with specific permissions for managing devices and automations.

## Architecture

The project follows a client-server architecture, where the server handles device management,
automation logic, and communication with clients. The client application provides a graphical
user interface for users to interact with the system.

Devices communicate with the server using a defined protocol,
allowing for seamless integration of various smart home devices. There are three types of devices:
controllers, sensors, and actuators. Controllers can control other devices, sensors provide data
to the server, and actuators perform actions based on commands from the server.

The network communication is handled using TCP sockets, and the server listens for incoming connections
from clients and devices. The server processes incoming messages, updates device states, and sends
responses back to the clients. The server also manages automations, executing actions based on predefined rules and conditions.

The network topology is a star topology, with the server at the center and clients/devices connecting to it.
This allows for efficient communication and centralized control of the smart home system.

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
mvn compile
````

**Running the application:**

The _server_ can be started from: [Main.java](src/main/java/edu/ntnu/bidata/group1/Main.java) or started in demo mode
(with example devices and room) from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

Running the control panel also from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

Note: The class Launcher can be found in the package `edu.ntnu.bidata.group1.gui` and is used to launch the GUI application.

## Protocol

The [PROTOCOL.md](PROTOCOL.md) describes how server and client communicates with each other and general description about the project.
- Transport protocol, address, port etc.
- Format of messages (also see [protocol.java](src/main/java/edu/ntnu/bidata/group1/utility/Protocol.java)).
- Types of controllers, sensors and actuators.
- Automations
- Rooms.

## Sprints
Sprints are found under [sprints](sprints/)

