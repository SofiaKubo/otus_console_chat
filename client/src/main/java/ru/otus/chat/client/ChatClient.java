package ru.otus.chat.client;

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

public class ChatClient {
    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader serverReader;
    private BufferedWriter serverWriter;

    private static final String EXIT_COMMAND = "exit";

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            connect();
            initializeStreams();
            startServerListener();
            readConsoleAndSendMessages();
        } catch (IOException e) {
            System.err.println(
                "[CLIENT] Failed to communicate with server at "
                    + host + ":" + port
                    + ": " + e.getMessage()
            );
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        System.out.println("[CLIENT] Connected to server at " + host + ": " + port + ".");
    }

    private void initializeStreams() throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        serverReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        serverWriter = new BufferedWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        );
    }

    private void startServerListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String message;

                while ((message = serverReader.readLine()) != null) {
                    System.out.println(message);
                }

                System.out.println("[CLIENT] Server closed connection.");
            } catch (IOException e) {
                System.err.println(
                    "[CLIENT] Failed to read message from server: " + e.getMessage());
            }
        }, "server-listener");

        listenerThread.start();
    }

    private void readConsoleAndSendMessages() throws IOException {
        BufferedReader consoleReader = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );

        while (true) {
            String message = consoleReader.readLine();

            if (message == null) {
                return;
            }

            sendMessage(message);

            if (EXIT_COMMAND.equalsIgnoreCase(message.trim())) {
                return;
            }
        }
    }

    private void sendMessage(String message) throws IOException {
        serverWriter.write(message);
        serverWriter.newLine();
        serverWriter.flush();
    }

    private void cleanup() {
        closeResource(serverWriter, "server writer");
        closeResource(serverReader, "server reader");
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
                "[CLIENT] Failed to close "
                    + resourceName
                    + ": "
                    + e.getMessage()
            );
        }
    }
}
