package com.template;

import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;

    private MessageListener listener;

    public interface MessageListener {
        void onMessage(String message);
    }

    public void setMessageListener(MessageListener l) {
        this.listener = l;
    }

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        Thread t = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    if (listener != null) listener.onMessage(line);
                }
            } catch (IOException e) {
                if (running) System.out.println("Disconnected from server.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
