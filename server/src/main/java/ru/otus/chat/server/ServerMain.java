package ru.otus.chat.server;

public class ServerMain {
    private static final int SERVER_PORT = 8091;

    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_PORT);
        server.start();
    }
}
