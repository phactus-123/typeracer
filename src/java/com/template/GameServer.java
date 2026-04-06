package com.template;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket[] clients = new Socket[2];
    private PrintWriter[] writers = new PrintWriter[2];
    private BufferedReader[] readers = new BufferedReader[2];
    private String[] usernames = new String[2];  // tracks username per slot

    private MessageListener listener;
    private volatile boolean running = false;

    public interface MessageListener {
        void onMessage(String message);
    }

    public void setMessageListener(MessageListener l) {
        this.listener = l;
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        for (int i = 0; i < 2; i++) {
            clients[i] = serverSocket.accept();
            writers[i] = new PrintWriter(clients[i].getOutputStream(), true);
            readers[i] = new BufferedReader(new InputStreamReader(clients[i].getInputStream()));
        }

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
                // Capture the username when the client first identifies itself
                if (line.startsWith("JOIN:")) {
                    usernames[idx] = line.substring("JOIN:".length()).trim();
                }
                broadcast(line);
                if (listener != null) listener.onMessage(line);
            }
        } catch (IOException e) {
            if (running) System.out.println("Client " + idx + " disconnected: " + e.getMessage());
        } finally {
            // Tell the remaining player that this client left
            if (running && usernames[idx] != null) {
                broadcast("DISCONNECT:" + usernames[idx]);
            }
            // Clean up this client's slot so broadcast() skips it
            try { if (clients[idx] != null) clients[idx].close(); } catch (IOException ignored) {}
            clients[idx]   = null;
            writers[idx]   = null;
            readers[idx]   = null;
            usernames[idx] = null;
        }
    }

    public void broadcast(String msg) {
        for (PrintWriter w : writers) {
            if (w != null) w.println(msg);
        }
    }

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
