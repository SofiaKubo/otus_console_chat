package ru.otus.chat.client;

public class ClientMain {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8091;

    public static void main(String[] args) {
        ChatClient client = new ChatClient(SERVER_HOST, SERVER_PORT);
        client.start();
    }
}
