package com.frommzay.chess.services.infrastructure.network;

import java.io.*;
import java.net.*;

/**
 * TCP host (server) implementation of {@link NetworkTransport}.
 *
 * Waits for a client, sends an initial "HELLO", and forwards incoming
 * lines to a listener.
 */
public class TcpHost implements NetworkTransport {
    private final int port;
    private volatile Listener listener;
    private ServerSocket server;
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private Thread acceptor;
    private Thread reader;

    public TcpHost(int port) { this.port = port; }

    @Override public void setListener(Listener l) { this.listener = l; }

    @Override public void start() {
        acceptor = new Thread(() -> {
            try {
                server = new ServerSocket(port);
                client = server.accept();
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                out.println("HELLO"); // handshake

                // Spawn reader thread for incoming messages
                reader = new Thread(() -> {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            var L = listener; if (L != null) L.onMessage(line);
                        }
                    } catch (Exception e) {
                        var L = listener; if (L != null) L.onError(e);
                    }
                }, "TcpHost-Reader");
                reader.setDaemon(true);
                reader.start();
            } catch (IOException e) {
                var L = listener; if (L != null) L.onError(e);
            }
        }, "TcpHost-Acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @Override public void send(String line) {
        if (out != null) out.println(line);
    }

    @Override public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (client != null) client.close(); } catch (IOException ignored) {}
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        if (reader != null) reader.interrupt();
        if (acceptor != null) acceptor.interrupt();
    }
}
