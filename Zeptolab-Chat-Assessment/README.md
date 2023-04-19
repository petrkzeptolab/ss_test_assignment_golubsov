# Chat Application

This is a Netty-based chat application with a simple command-line interface. Below are the available commands:

- `/login <name> <password>`: If the user doesn’t exist, create a new profile;
otherwise, log in and join the last connected channel (if any). If the client’s limit of active channels is exceeded, keep the connection open but without an active channel.
- `/join <channel>`: Try to join a channel (with a maximum of 10 active clients per channel).
If the client's limit is exceeded, send an error message; 
otherwise, join the channel and send the last N messages of activity.
- `/leave`: Leave the current channel.
- `/disconnect`: Close the connection to the server.
- `/list`: Send a list of available channels.
- `/users`: Send a list of unique users in the current channel.
- `<text message terminated with CR>`: Send a message to the current channel. 
The server must broadcast this message to all clients connected to this channel.

## Prerequisites
- Java 17 or higher
- Apache Maven 3.6.3 or higher

## Building
To build the chat application, run the following command from the project root directory:

```sh
mvn package
```
This will build the project and generate an executable JAR file in the target/ directory.

## Running
To run the chat application, execute the following command:
```sh
java -jar target/Zeptolab-Chat-Assessment-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Usage examples
After starting the server, you can connect to it using a Telnet client or any other tool that 
supports TCP connections. To connect to the server, use the following command:
```sh
telnet <host> 8080
```
Replace `<host>` with the hostname or IP address of the machine running the server

Once connected, you can start using the chat application by entering one of the available commands listed above. 
But first you need to login. For example to log in with the username "john" and the password "password", you would enter the following command:
```sh
/login john password
```
If the user does not exist, a new profile will be created with the given username and password. 
If the user already exists, the server will check the provided password and log in the user if it is correct. 
The user will then automatically join the last connected channel (if any).

Then you have to connect to the channel. For example you want to connect to channel "test". You would have to enter the following command:
```sh
/join test
```
If the channel doesn't exist, it will be created and you will join it.
If channel exists, depending on whether it has reached it max amount of users or not, you will connect to it or get an error.

To send a message to the current channel, simply type your message and hit enter. 