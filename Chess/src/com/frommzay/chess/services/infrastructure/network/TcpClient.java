package com.frommzay.chess.services.infrastructure.network;

import java.io.*;
import java.net.Socket;

/**
 * TCP client implementation of {@link NetworkTransport}.
 *
 * Connects to a host:port, sends an initial "HELLO", and forwards
 * incoming lines to a listener.
 */
public class TcpClient implements NetworkTransport {
    private final String host;
    private final int port;
    private volatile Listener listener;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread reader;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override public void setListener(Listener l) { this.listener = l; }

    @Override public void start() {
        reader = new Thread(() -> {
            try {
                socket = new Socket(host, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("HELLO"); // handshake
                String line;
                while ((line = in.readLine()) != null) {
                    var L = listener; if (L != null) L.onMessage(line);
                }
            } catch (Exception e) {
                var L = listener; if (L != null) L.onError(e);
            }
        }, "TcpClient-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    @Override public void send(String line) {
        if (out != null) out.println(line);
    }

    @Override public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (reader != null) reader.interrupt();
    }
}
