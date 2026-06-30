package ru.otus.chat.server;

public class ServerMain {
    public static void main(String[] args) {
        final int SERVER_PORT = 8091;
        ChatServer server = new ChatServer(SERVER_PORT);
        server.start();
    }
}
