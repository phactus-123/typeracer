package com.template;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket[] clients = new Socket[2];
    private PrintWriter[] writers = new PrintWriter[2];
    private BufferedReader[] readers = new BufferedReader[2];

    private MessageListener listener;
    private volatile boolean running = false;

    public interface MessageListener {
        void onMessage(String message);
    }

    public void setMessageListener(MessageListener l) {
        this.listener = l;
    }

    /**
     * Starts the server, waits for exactly 2 clients to connect,
     * then starts listening threads for both.
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        // Accept both clients sequentially
        for (int i = 0; i < 2; i++) {
            clients[i] = serverSocket.accept();
            writers[i] = new PrintWriter(clients[i].getOutputStream(), true);
            readers[i] = new BufferedReader(new InputStreamReader(clients[i].getInputStream()));
        }

        // Start a listener thread for each client
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            Thread t = new Thread(() -> listenFrom(idx));
            t.setDaemon(true);
            t.start();
        }
    }

    private void listenFrom(int idx) {
        try {
            String line;
            while (running && (line = readers[idx].readLine()) != null) {
                // Broadcast every message from one client to the other (and to local listener)
                broadcast(line);
                if (listener != null) listener.onMessage(line);
            }
        } catch (IOException e) {
            if (running) System.out.println("Client " + idx + " disconnected.");
        }
    }

    /** Sends a message to both connected clients. */
    public void broadcast(String msg) {
        for (PrintWriter w : writers) {
            if (w != null) w.println(msg);
        }
    }

    /** Sends a message to a specific client index (0 or 1). */
    public void sendTo(int idx, String msg) {
        if (writers[idx] != null) writers[idx].println(msg);
    }

    public void stop() {
        running = false;
        try {
            for (Socket s : clients) if (s != null) s.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
