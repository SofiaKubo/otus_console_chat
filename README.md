# OTUS Console Chat

A simple multi-threaded console chat application written in Java.

The project contains a TCP server and console clients. Several clients can connect to the server at the same time, register unique nicknames, send public messages to the chat, send private messages to a selected user, and leave the chat with the `exit` command.

This project was implemented as a learning task for practicing Java networking, sockets, streams, and multithreading.

## Main Features

- Multiple clients can connect to one server.
- Each connected client is handled in a separate server-side thread.
- Each client chooses a unique nickname.
- Duplicate nicknames are rejected.
- Public messages are delivered to all online users except the sender.
- Private messages are supported with the `/w` command.
- System messages notify users when someone joins or leaves the chat.
- Empty messages are ignored.
- The `exit` command closes the client connection gracefully.
- Basic validation and error handling are implemented.

## Project Structure

```text
otus-console-chat
│
├── pom.xml
│
├── client
│   ├── pom.xml
│   └── src/main/java/ru/otus/chat/client
│       ├── ClientMain.java
│       └── ChatClient.java
│
└── server
    ├── pom.xml
    └── src/main/java/ru/otus/chat/server
        ├── ServerMain.java
        ├── ChatServer.java
        └── ClientHandler.java
```

## Modules

### `server`

The server module contains all server-side logic.

It accepts client connections, starts a separate handler thread for each client, stores registered users, routes messages, broadcasts system messages, and sends private messages.

### `client`

The client module contains the console client.

It connects to the server, reads user input from the console, sends messages to the server, and listens for messages from the server in a separate thread.

## Server Components

### `ServerMain`

The entry point of the server application.

Responsibilities:

- Defines the server port.
- Creates a `ChatServer` instance.
- Starts the server.

### `ChatServer`

The main server class.

Responsibilities:

- Opens a `ServerSocket`.
- Accepts incoming client connections.
- Creates and starts a `ClientHandler` thread for each client.
- Stores registered clients in a thread-safe `ConcurrentHashMap`.
- Registers clients by nickname.
- Rejects duplicate nicknames.
- Removes clients when they disconnect.
- Handles public messages.
- Handles private messages.
- Sends system messages.
- Safely sends messages to clients.

The server stores online clients in this map:

```java
ConcurrentMap<String, ClientHandler> clients
```

The key is the user's nickname, and the value is the corresponding `ClientHandler`.

### `ClientHandler`

A server-side handler for one connected client.

Each connected client gets its own `ClientHandler`, and each handler runs in a separate thread.

Responsibilities:

- Initializes input and output streams for one socket connection.
- Reads the client's nickname.
- Registers the client on the server.
- Reads messages from the client in a loop.
- Handles the `exit` command.
- Passes regular messages to `ChatServer`.
- Removes the client from the server during cleanup.
- Closes socket resources.

## Client Components

### `ClientMain`

The entry point of the client application.

Responsibilities:

- Defines the server host and port.
- Creates a `ChatClient` instance.
- Starts the client.

### `ChatClient`

The main client class.

Responsibilities:

- Connects to the server using a socket.
- Initializes input and output streams.
- Starts a separate listener thread for reading messages from the server.
- Reads user input from the console.
- Sends user messages to the server.
- Handles graceful shutdown.
- Closes socket resources.

The client uses two threads:

1. The main thread reads console input and sends messages to the server.
2. The listener thread reads messages from the server and prints them to the console.

## How It Works

### Connection Flow

1. Start the server.
2. The server opens a `ServerSocket` on the configured port.
3. Start one or more clients.
4. Each client connects to the server socket.
5. The server accepts the connection.
6. The server creates a `ClientHandler` for the new client.
7. The `ClientHandler` asks the client to enter a nickname.
8. The nickname is registered if it is valid and unique.
9. Other online users receive a system message when the client joins the chat.
10. The client can start sending messages.

### Message Flow

When a client sends a normal message:

```text
Hello everyone!
```

The server formats it with the sender nickname:

```text
tom: Hello everyone!
```

Then the server sends it to all online users except the sender.

### Private Message Flow

Private messages use this format:

```text
/w <nickname> <message>
```

Example:

```text
/w kate Hello, Kate!
```

The recipient sees:

```text
[private from tom] Hello, Kate!
```

The sender sees confirmation:

```text
[private to kate] Hello, Kate!
```

Other users do not receive this message.

### Exit Flow

When a client sends:

```text
exit
```

The server sends:

```text
Goodbye.
```

Then the client disconnects, and the server removes the user from the online users map.

Other users receive a system message:

```text
[SYSTEM] User tom left the chat.
```

## Supported Commands

### Exit the chat

```text
exit
```

Closes the client connection gracefully.

### Send a private message

```text
/w <nickname> <message>
```

Example:

```text
/w lucy Hi, Lucy!
```

### Unknown commands

Any other message starting with `/` is treated as an unknown command.

Example:

```text
/help
```

Response:

```text
Unknown command: /help
```

## Validation Rules

### Nickname validation

A nickname:

- Cannot be `null`.
- Cannot be blank.
- Cannot contain spaces.
- Cannot start with `/`.
- Must be unique among currently connected users.

### Message validation

- Empty messages are ignored.
- Private messages must include both a recipient nickname and message text.
- Private messages cannot be sent to offline users.
- Users cannot send private messages to themselves.

## How to Run

### 1. Build the project

From the project root directory:

```bash
mvn clean package
```

or:

```bash
mvn clean compile
```

### 2. Start the server

Run:

```bash
java -cp server/target/classes ru.otus.chat.server.ServerMain
```

Or start `ServerMain` from the IDE.

Expected output:

```text
[SERVER] Starting server on port 8091.
[SERVER] Started on port 8091.
[SERVER] Waiting for clients...
```

### 3. Start clients

Open several terminal windows or run several client configurations in the IDE.

Run:

```bash
java -cp client/target/classes ru.otus.chat.client.ClientMain
```

Or start `ClientMain` from the IDE several times.

Each client will be asked to enter a nickname:

```text
Enter your nickname:
```

Example:

```text
tom
Welcome, tom! You joined the chat.
```

If other users are already online, they receive:

```text
[SYSTEM] User tom joined the chat.
```

## Example Session

### Client `tom`

```text
[CLIENT] Connected to server at localhost:8091.
Enter your nickname:
tom
Welcome, tom! You joined the chat.
Hello everyone!
/w kate Hi, Kate!
[private to kate] Hi, Kate!
exit
Goodbye.
```

### Client `kate`

```text
[CLIENT] Connected to server at localhost:8091.
Enter your nickname:
kate
Welcome, kate! You joined the chat.
[SYSTEM] User tom joined the chat.
tom: Hello everyone!
[private from tom] Hi, Kate!
[SYSTEM] User tom left the chat.
```

### Server

```text
[SERVER] Starting server on port 8091.
[SERVER] Started on port 8091.
[SERVER] Waiting for clients...
[SERVER] Connection accepted: /127.0.0.1:53637
[SERVER] Client tom registered.
[SERVER] Connection accepted: /127.0.0.1:53641
[SERVER] Client kate registered.
[SERVER] User tom left. Online users: 1
```

## Threading Model

The application uses multithreading on both the server and client sides.

### Server side

The server has one main thread that accepts new socket connections.

For every connected client, the server creates a separate `ClientHandler` thread.

This allows several clients to communicate with the server at the same time.

### Client side

Each client has:

- A main thread that reads console input.
- A listener thread that reads messages from the server.

This allows the client to send messages and receive messages independently.

## Networking

The application uses TCP sockets.

Server side:

- `ServerSocket` listens for incoming connections.
- `Socket` represents one client connection.

Client side:

- `Socket` connects to the server.

Data is sent as text lines using UTF-8 encoding.

## Error Handling

The application handles common error cases:

- Duplicate nickname.
- Invalid nickname.
- Unknown command.
- Invalid private message format.
- Offline private message recipient.
- Client disconnection.
- Failed message sending.

Message sending is wrapped in a safe helper method so that a failure while sending to one client does not stop the whole server.

## Technologies

- Java 21
- Maven
- Java Sockets
- Java IO streams
- Multithreading
- `ConcurrentHashMap`

## Notes

This is a console-based educational project. It is intentionally simple and does not include authentication, message history, encryption, database storage, or a graphical user interface.

The goal of the project is to practice:

- TCP client-server communication.
- Working with sockets.
- Using input and output streams.
- Running multiple threads.
- Coordinating shared data between threads.
- Separating responsibilities between classes.

## Code Style and Formatting

This project uses:

- `Spotless` for automatic Java formatting
- Eclipse JDT formatter for configurable Java line wrapping and spacing
- `Checkstyle` for style validation

### Check formatting and style

Run:

```bash
mvn verify -DskipTests
```

This will:

- check Java formatting with `spotless:check`
- validate style rules with `checkstyle:check`

### Auto-format the code

Run:

```bash
mvn spotless:apply
```

This will automatically format Java source files according to the project rules.

### Notes

- Formatting and style checks are configured in the root `pom.xml`
- Java formatter settings are stored in `config/formatter/eclipse-java-formatter.xml`
- Editor defaults are defined in `.editorconfig`
- Checkstyle rules are stored in `config/checkstyle/checkstyle.xml`
