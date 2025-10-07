package com.bcbs239.regtech.core.saga;

/**
 * Message bus interface for inter-bounded-context communication.
 * Supports both synchronous and asynchronous message delivery.
 */
public interface MessageBus {

    /**
     * Publishes a message to the bus (fire-and-forget)
     */
    void publish(SagaMessage message);

    /**
     * Sends a message and waits for a response
     */
    SagaMessage sendAndReceive(SagaMessage message);

    /**
     * Subscribes to messages of a specific type
     */
    void subscribe(String messageType, MessageHandler handler);

    /**
     * Unsubscribes from messages of a specific type
     */
    void unsubscribe(String messageType, MessageHandler handler);

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
    interface MessageHandler {
        void handle(SagaMessage message);
    }
}