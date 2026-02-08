package com.jeremyzay.zaychess.services.infrastructure.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Connects to the centralized Relay Server for matchmaking.
 * Protocol:
 * - Sends: {"type": "JOIN_QUEUE"}
 * - Receives: {"type": "MATCH_FOUND", "role": "HOST"|"CLIENT"}
 * - Relays moves: {"type": "MOVE", "data": "..."}
 */
public class RelayClient implements NetworkTransport {

    private final String serverHost;
    private final int serverPort;
    private volatile Listener listener;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread readerThread;

    // We need to notify the UI when a match is found, not just when a "move" comes
    // in.
    // The NetworkTransport.Listener interface only has onMessage(String).
    // The existing app expects `onMessage` to be game protocol messages.
    // So we will intercept the "MATCH_FOUND" message here, handle the setup,
    // and then expose a way for the UI to know the game started.

    // However, to keep it compatible with existing `NetworkTransport` usage:
    // The `Listener` is likely the GameController or GameView that starts the game.
    // We might need a custom interface for the matchmaking phase.

    private final java.util.concurrent.atomic.AtomicBoolean matchFound = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    public interface MatchmakingListener {
        void onMatchFound(boolean isHost);

        void onMatchmakingError(String msg);
    }

    private MatchmakingListener matchListener;

    public RelayClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void setMatchmakingListener(MatchmakingListener l) {
        this.matchListener = l;
    }

    @Override
    public void setListener(Listener l) {
        this.listener = l;
    }

    @Override
    public void start() {
        readerThread = new Thread(this::runLoop, "RelayClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void runLoop() {
        try {
            socket = new Socket(serverHost, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 1. Join Queue automatically on connect
            sendJson("JOIN_QUEUE", null);

            String line;
            while ((line = in.readLine()) != null) {
                // Parse JSON
                // Simple parsing to avoid dragging in a huge JSON lib if not present
                // (Though project likely has GSON or similar? Checking dependencies would be
                // good,
                // but simple string check is robust enough for this tiny protocol)

                if (line.contains("\"type\":\"MATCH_FOUND\"") || line.contains("\"type\": \"MATCH_FOUND\"")) {
                    boolean isHost = line.contains("\"role\":\"HOST\"") || line.contains("\"role\": \"HOST\"");

                    // Only fire callback once
                    if (matchFound.compareAndSet(false, true)) {
                        if (matchListener != null) {
                            matchListener.onMatchFound(isHost);
                        }
                    }
                } else if (line.contains("\"type\":\"MOVE\"") || line.contains("\"type\": \"MOVE\"")) {
                    // Extract "data" content.
                    // Quick hacky parse to avoid dependency issues if none exist:
                    // Assuming format: {"type":"MOVE","data":"..."}
                    int dataIndex = line.indexOf("\"data\":");
                    if (dataIndex != -1) {
                        String content = line.substring(dataIndex + 7).trim(); // "..."} or "..."
                        if (content.startsWith("\"")) {
                            content = content.substring(1); // remove leading quote
                            // remove trailing quote and brace
                            int lastQuote = content.lastIndexOf("\"");
                            if (lastQuote != -1) {
                                content = content.substring(0, lastQuote);
                                // Unescape if needed, but for simple chess moves likely fine
                                if (listener != null)
                                    listener.onMessage(content);
                            }
                        }
                    }
                } else if (line.contains("\"type\":\"OPPONENT_LEFT\"")) {
                    // Handle disconnect
                    if (listener != null)
                        listener.onError(new IOException("Opponent disconnected"));
                }
            }
        } catch (IOException e) {
            if (matchListener != null)
                matchListener.onMatchmakingError("Connection error: " + e.getMessage());
            if (listener != null)
                listener.onError(e);
        }
    }

    @Override
    public void send(String msg) {
        // Wrap game messages in our MOVE protocol
        // Escape quotes in msg if necessary (simple approach)
        String escaped = msg.replace("\"", "\\\"");
        sendJson("MOVE", escaped);
    }

    private void sendJson(String type, String data) {
        if (out != null) {
            String json;
            if (data == null) {
                json = String.format("{\"type\":\"%s\"}", type);
            } else {
                json = String.format("{\"type\":\"%s\",\"data\":\"%s\"}", type, data);
            }
            out.println(json);
        }
    }

    @Override
    public void close() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
        if (readerThread != null)
            readerThread.interrupt();
    }
}
