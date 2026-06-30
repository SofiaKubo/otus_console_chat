package ru.otus.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatServer {
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
            System.out.println("[SERVER] Waiting for connections.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket
                    .getRemoteSocketAddress()
                    .toString();

                System.out.println("[SERVER] Client connected: " + clientAddress);
                try {
                    ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        this
                    );
                    Thread thread = new Thread(
                        clientHandler,
                        "client-handler-" + clientAddress
                    );

                    System.out.println(
                        "[SERVER] Client handler created for: "
                            + clientAddress
                    );
                    thread.start();
                    System.out.println(
                        "[SERVER] Client handler thread started for: "
                            + clientAddress
                    );
                } catch (RuntimeException e) {
                    System.err.println(
                        "[SERVER] Failed to start client handler for: "
                            + clientAddress
                    );
                    System.err.println("[SERVER] Reason: " + e.getMessage());
                    closeClientSocket(clientSocket);
                }
            }
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
            System.out.println("[SERVER] Nickname is already taken: " + nickname);
            return false;
        }
        System.out.println("[SERVER] Client registered: " + nickname);
        return true;
    }

    public void removeClient(String nickname) {
        if (nickname == null) {
            return;
        }

        ClientHandler removedClient = clients.remove(nickname);

        if (removedClient != null) {
            System.out.println("[SERVER] Client disconnected: " + nickname);
            broadcastSystemMessage(nickname + " left the chat.");
        }
    }

    public void handleMessage(ClientHandler sender, String message) {
    }

    private void broadcastMessage(String message) {
    }

    private void handlePrivateMessage(ClientHandler sender, String message) {
    }

    private void broadcastSystemMessage(String message) {
    }

    private void sendMessageSafely(ClientHandler clientHandler, String message) {
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
