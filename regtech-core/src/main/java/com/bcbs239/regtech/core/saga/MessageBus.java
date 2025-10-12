package com.bcbs239.regtech.core.saga;

/**
 * Message bus interface for inter-bounded-context communication.
 * Supports both synchronous and asynchronous message delivery.
 */
public interface MessageBus<T extends Message> {

    /**
     * Publishes a message to the bus (fire-and-forget)
     */
    void publish(T message);

    /**
     * Sends a message and waits for a response
     */
    T sendAndReceive(T message);

    /**
     * Subscribes to messages of a specific type
     */
    void subscribe(String messageType, MessageHandler<T> handler);

    /**
     * Unsubscribes from messages of a specific type
     */
    void unsubscribe(String messageType, MessageHandler<T> handler);

    /**
     * Starts the message bus
     */
    void start();

    /**
     * Stops the message bus
     */
    void stop();

    /**
     * Message handler interface
     */
    @FunctionalInterface
    interface MessageHandler<T extends Message> {
        void handle(T message);
    }
}