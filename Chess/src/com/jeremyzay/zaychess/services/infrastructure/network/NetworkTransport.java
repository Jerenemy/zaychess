package com.jeremyzay.zaychess.services.infrastructure.network;

/**
 * Abstraction for network transport (client or host).
 *
 * Provides methods for setting a listener, starting the transport,
 * sending messages, and closing resources.
 */
public interface NetworkTransport extends AutoCloseable {
    /**
     * Listener for incoming messages and errors.
     */
    interface Listener {
        /** Called when a new line/message arrives. */
        void onMessage(String line);

        /** Called on transport errors (optional). */
        default void onError(Exception e) {}
    }

    /** Set the event listener. */
    void setListener(Listener l);

    /** Start the transport asynchronously (non-blocking). */
    void start();

    /** Send a line to the peer. */
    void send(String line);

    /** Close resources (socket, streams, threads). */
    @Override void close();
}
