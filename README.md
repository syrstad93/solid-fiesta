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

| Package | Version           |
|---------|---        |
| Java    | 25        |
| JavaFX  | 25.0.1    |
| Maven   | 25        |
| JUnit   | 5.10.2    |

## How to run the application

**Compiling the project:**
````bash
mvn compile
````

**Running the application:**

The _server_ can be started from: [Main.java](src/main/java/edu/ntnu/bidata/group1/Main.java) or started in demo mode
(with example devices and room) from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

Running the control panel also from: [Launcher.java](src/main/java/edu/ntnu/bidata/group1/gui/Launcher.java).

> Note: The class Launcher can be found in the package `edu.ntnu.bidata.group1.gui` and is used to launch the GUI application.
> It is not necessary to run the server separately, as there is an option on the front page
> of the GUI to start the server in demo mode. This will automatically create example
> devices and a room for testing purposes.

## Protocol

The [PROTOCOL.md](PROTOCOL.md) describes how server and client communicates with each other and general description about the project.
- Transport protocol, address, port etc.
- Format of messages (also see [protocol.java](src/main/java/edu/ntnu/bidata/group1/utility/Protocol.java)).
- Types of controllers, sensors and actuators.
- Automations
- Rooms.

## Project Structure

The project is organized into the following main packages:
- `edu.ntnu.bidata.group1`: Contains the main classes for the project, including the server and client implementations.
- `edu.ntnu.bidata.group1.gui`: Contains classes related to the graphical user interface (GUI) of the application.
- `edu.ntnu.bidata.group1.devices`: Contains classes representing different types of smart home devices, including controllers, sensors, and actuators.
- `edu.ntnu.bidata.group1.network`: Contains classes related to network communication, including socket handling and message processing.
- `edu.ntnu.bidata.group1.utility`: Contains utility classes and helper functions used throughout the project.

## Testing

The project includes unit tests for various components, ensuring the correctness and reliability of the system.

Tests can be run using the following Maven command:
````bash
mvn test
````

The tests cover different aspects of the application, including device management, automation logic, and network communication.

The application has also been user tested by the group members,
and feedback has been collected to improve the user experience and functionality of the system.

## Development Process

A combined PDF document of all sprint plans and sprint reviews are uploaded to Inspera by each group member.
It is also uploaded to the project directory in a folder called `doc`.

The project directory 'doc' contains the following files:
- `FullSprintsDoc.pdf`: Sprint 1 plan and review.
- `Planning Process.pdf`: A document showing how the group planned and organized the development process outside the sprints.
- **Class Diagrams**: Class diagrams for the project, showing the relationships between different classes and their attributes/methods.
- **Sequence Diagrams**: Sequence diagrams illustrating the interactions between different components of the system during various operations.
- **Component Diagrams**: Component diagrams showing the high-level structure of the system and how different components interact with each other.
- **Topology Illustration**: A visual representation of the network topology used in the smart home automation system.

## AI Disclaimer

This project was developed with the assistance of AI tools, including ChatGPT and GitHub Copilot,
which provided guidance and suggestions during the development process.
While AI tools were used to enhance productivity and provide insights,
the final implementation and decisions were made by the group members.
The group members take full responsibility for the content and functionality of the project.
> Exception: The entirety of gui package is generated by AI.