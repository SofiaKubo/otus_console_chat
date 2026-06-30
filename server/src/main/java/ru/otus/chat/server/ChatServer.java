package ru.otus.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatServer {
    private static final String PRIVATE_MESSAGE_COMMAND = "/w";
    private static final String PRIVATE_MESSAGE_USAGE = "Usage: /w <nickname> <message>";

    private final int port;
    private final ConcurrentMap<String, ClientHandler> clients;

    public ChatServer(int port) {
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
    }

    public void start() {
        System.out.println("[SERVER] Starting server on port " + port + ".");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Started on port " + port + ".");
            System.out.println("[SERVER] Waiting for clients...");

            acceptClients(serverSocket);
        } catch (IOException e) {
            System.err.println("[SERVER] Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean registerClient(String nickname, ClientHandler clientHandler) {
        validateNickname(nickname);

        if (clientHandler == null) {
            throw new IllegalArgumentException("ClientHandler cannot be null.");
        }

        ClientHandler previousClient = clients.putIfAbsent(
            nickname,
            clientHandler
        );

        if (previousClient != null) {
            System.out.println(
                "[SERVER] Nickname "
                    + nickname
                    + " is already taken."
            );
            return false;
        }

        System.out.println("[SERVER] Client " + nickname + " registered.");
        return true;
    }

    public void removeClient(String nickname) {
        if (nickname == null) {
            return;
        }

        ClientHandler removedClient = clients.remove(nickname);

        if (removedClient != null) {
            System.out.println(
                "[SERVER] User "
                    + nickname
                    + " left. Online users: "
                    + clients.size()
            );
            broadcastSystemMessage("User " + nickname + " left the chat.");
        }
    }

    public void handleMessage(ClientHandler sender, String message) {
        if (!isValidSenderMessageData(sender, message, "handle message")) {
            return;
        }

        String senderNickname = sender.getNickname();

        if (isPrivateMessageCommand(message)) {
            handlePrivateMessage(sender, message);
            return;
        }

        if (message.startsWith("/")) {
            sendMessageSafely(sender, "Unknown command: " + message);
            return;
        }

        String preparedMessage = senderNickname + ": " + message;
        broadcastMessage(sender, preparedMessage);
    }

    private void acceptClients(ServerSocket serverSocket) throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            startClientHandler(clientSocket);
        }
    }

    private void startClientHandler(Socket clientSocket) {
        String clientAddress = getClientAddress(clientSocket);

        System.out.println("[SERVER] Connection accepted: " + clientAddress);

        try {
            ClientHandler clientHandler = new ClientHandler(clientSocket, this);
            Thread thread = createClientThread(clientHandler, clientAddress);

            thread.start();
        } catch (RuntimeException e) {
            System.err.println(
                "[SERVER] Failed to start client handler for: "
                    + clientAddress
            );
            System.err.println("[SERVER] Reason: " + e.getMessage());
            closeClientSocket(clientSocket);
        }
    }

    private Thread createClientThread(
        ClientHandler clientHandler,
        String clientAddress
    ) {
        return new Thread(
            clientHandler,
            "client-handler-" + clientAddress
        );
    }

    private String getClientAddress(Socket clientSocket) {
        return String.valueOf(clientSocket.getRemoteSocketAddress());
    }

    private void broadcastMessage(ClientHandler sender, String message) {
        if (!isValidSenderMessageData(sender, message, "broadcast message")) {
            return;
        }

        if (clients.isEmpty()) {
            return;
        }

        for (ClientHandler clientHandler : clients.values()) {
            if (clientHandler == sender) {
                continue;
            }
            sendMessageSafely(clientHandler, message);
        }
    }

    private void handlePrivateMessage(ClientHandler sender, String message) {
        if (!isValidSenderMessageData(sender, message, "handle private message")) {
            return;
        }

        String[] messageTokens = message.split("\\s+", 3);

        if (messageTokens.length < 3) {
            sendPrivateMessageUsage(sender);
            return;
        }

        String senderNickname = sender.getNickname();
        String recipientNickname = messageTokens[1];
        String privateMessage = messageTokens[2].trim();

        if (privateMessage.isBlank()) {
            sendPrivateMessageUsage(sender);
            return;
        }

        ClientHandler recipient = clients.get(recipientNickname);

        if (recipient == null) {
            sendMessageSafely(sender, "User " + recipientNickname + " is not online.");
            return;
        }

        if (recipient == sender) {
            sendMessageSafely(
                sender,
                "You cannot send a private message to yourself."
            );
            return;
        }

        sendMessageSafely(
            recipient,
            "[private from " + senderNickname + "] " + privateMessage
        );
        sendMessageSafely(
            sender,
            "[private to " + recipientNickname + "] " + privateMessage
        );
    }

    private void broadcastSystemMessage(String message) {
        if (message == null) {
            System.err.println("[SERVER] Cannot broadcast system message: message is null.");
            return;
        }

        if (clients.isEmpty()) {
            return;
        }

        String preparedMessage = "[SYSTEM] " + message;

        for (ClientHandler clientHandler : clients.values()) {
            sendMessageSafely(clientHandler, preparedMessage);
        }
    }

    private void sendPrivateMessageUsage(ClientHandler sender) {
        sendMessageSafely(sender, PRIVATE_MESSAGE_USAGE);
    }

    private boolean isPrivateMessageCommand(String message) {
        return PRIVATE_MESSAGE_COMMAND.equals(message)
            || message.startsWith(PRIVATE_MESSAGE_COMMAND + " ");
    }

    private boolean isValidSenderMessageData(
        ClientHandler sender,
        String message,
        String action
    ) {
        if (sender == null) {
            System.err.println("[SERVER] Cannot " + action + ": sender is null.");
            return false;
        }

        if (message == null) {
            System.err.println("[SERVER] Cannot " + action + ": message is null.");
            return false;
        }

        if (sender.getNickname() == null) {
            System.err.println(
                "[SERVER] Cannot "
                    + action
                    + ": sender is not registered."
            );
            return false;
        }

        return true;
    }

    private void sendMessageSafely(ClientHandler clientHandler, String message) {
        if (clientHandler == null) {
            System.err.println("[SERVER] Cannot send message: client handler is null.");
            return;
        }

        if (message == null) {
            System.err.println("[SERVER] Cannot send message: message is null.");
            return;
        }

        try {
            clientHandler.sendMessage(message);
        } catch (IOException e) {
            System.err.println(
                "[SERVER] Failed to send message to user "
                    + clientHandler.getNickname()
                    + ": "
                    + e.getMessage()
            );
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null) {
            throw new IllegalArgumentException("Nickname cannot be null.");
        }
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be empty.");
        }
        if (nickname.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Nickname cannot contain spaces.");
        }
        if (nickname.startsWith("/")) {
            throw new IllegalArgumentException(
                "Nickname cannot start with a slash('/')."
            );
        }
    }

    private void closeClientSocket(Socket clientSocket) {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println(
                    "[SERVER] Failed to close client socket: "
                        + e.getMessage()
                );
            }
        }
    }
}
