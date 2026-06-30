package ru.otus.chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

    @Override
    public void run() {
        try {
            initializeStreams();

            if (!registerNickname()) {
                System.out.println(
                        "[SERVER] Client disconnected before registration: " + socket.getRemoteSocketAddress());
                return;
            }
            readMessagesLoop();
        } catch (IOException e) {
            System.err.println("[SERVER] Client communication error " + getClientDescription() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void initializeStreams() throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
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
                    sendMessage("You joined the chat as " + nickname + ".");
                    return true;
                }
                sendMessage("Nickname is already taken. Try again.");
            } catch (IllegalArgumentException e) {
                sendMessage(e.getMessage());
            }
        }
    }

    private void readMessagesLoop() throws IOException {
        System.out.println("[SERVER] Started reading messages from " + getClientDescription() + ".");

        while (true) {
            String message = reader.readLine();

            if (message == null) {
                System.out.println("[SERVER] Client disconnected: " + getClientDescription());
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
        // добавим следующим шагом
    }

    public synchronized void sendMessage(String message) throws IOException {
        this.writer.write(message);
        this.writer.newLine();
        this.writer.flush();
    }

    private String getClientDescription() {
        if (nickname != null) {
            return "'" + nickname + "'";
        }

        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
