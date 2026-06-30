package ru.otus.chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String nickname;

    private static final String EXIT_COMMAND = "exit";

    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.socket = clientSocket;
        this.server = server;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void run() {
        try {
            initializeStreams();

            if (!registerNickname()) {
                System.out.println(
                    "[SERVER] Client disconnected before registration: "
                        + socket.getRemoteSocketAddress()
                );
                return;
            }

            readMessagesLoop();
        } catch (IOException e) {
            System.err.println(
                "[SERVER] Client "
                    + getClientDescription()
                    + " disconnected unexpectedly. Reason: "
                    + e.getMessage()
            );
        } finally {
            cleanup();
        }
    }

    private void initializeStreams() throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        writer = new BufferedWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        );
    }

    private boolean registerNickname() throws IOException {
        while (true) {
            sendMessage("Enter your nickname: ");
            String candidate = reader.readLine();

            if (candidate == null) {
                return false;
            }
            candidate = candidate.trim();

            try {
                boolean registered = server.registerClient(candidate, this);

                if (registered) {
                    this.nickname = candidate;
                    sendMessage("Welcome, " + nickname + "! You joined the chat.");
                    return true;
                }

                sendMessage("Nickname " + candidate + " is already taken. Try again.");
            } catch (IllegalArgumentException e) {
                sendMessage(e.getMessage());
            }
        }
    }

    private void readMessagesLoop() throws IOException {
        while (true) {
            String message = reader.readLine();

            if (message == null) {
                return;
            }
            message = message.trim();

            if (message.isEmpty()) {
                continue;
            }

            if (EXIT_COMMAND.equalsIgnoreCase(message)) {
                sendMessage("Goodbye.");
                return;
            }

            server.handleMessage(this, message);
        }
    }

    private void cleanup() {
        if (nickname != null) {
            server.removeClient(nickname);
        }

        closeResource(writer, "writer");
        closeResource(reader, "reader");
        closeResource(socket, "socket");
    }

    private void closeResource(Closeable resource, String resourceName) {
        if (resource == null) {
            return;
        }

        try {
            resource.close();
        } catch (IOException e) {
            System.err.println(
                "[SERVER] Failed to close "
                    + resourceName
                    + " for client "
                    + getClientDescription()
                    + ": "
                    + e.getMessage()
            );
        }
    }

    public synchronized void sendMessage(String message) throws IOException {
        this.writer.write(message);
        this.writer.newLine();
        this.writer.flush();
    }

    private String getClientDescription() {
        if (nickname != null) {
            return nickname;
        }

        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
