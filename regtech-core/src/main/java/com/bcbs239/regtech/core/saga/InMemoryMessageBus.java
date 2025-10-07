package com.bcbs239.regtech.core.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * In-memory message bus implementation for modular monolith.
 * Routes messages between bounded contexts within the same JVM.
 */
@Component
public class InMemoryMessageBus implements MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryMessageBus.class);

    private final Map<String, List<MessageHandler>> subscribers = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    @Override
    public void publish(SagaMessage message) {
        if (!running) {
            logger.warn("Message bus is not running, cannot publish message: {}", message);
            return;
        }

        logger.debug("Publishing message: {}", message);
        List<MessageHandler> handlers = subscribers.get(message.getType());
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    handler.handle(message);
                } catch (Exception e) {
                    logger.error("Error handling message: {}", message, e);
                }
            });
        }
    }

    @Override
    public SagaMessage sendAndReceive(SagaMessage message) {
        // For in-memory bus, we simulate synchronous behavior
        // In a real distributed system, this would use request-reply patterns
        logger.debug("Sending and receiving message: {}", message);

        // For now, return a simple acknowledgment
        // In practice, this would wait for a response message
        return SagaMessage.builder()
                .sagaId(message.getSagaId())
                .messageId("response-" + message.getMessageId())
                .type(message.getType() + ".response")
                .source(message.getTarget())
                .target(message.getSource())
                .payload("ACK")
                .build();
    }

    @Override
    public void subscribe(String messageType, MessageHandler handler) {
        subscribers.computeIfAbsent(messageType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("Subscribed handler for message type: {}", messageType);
    }

    @Override
    public void unsubscribe(String messageType, MessageHandler handler) {
        List<MessageHandler> handlers = subscribers.get(messageType);
        if (handlers != null) {
            handlers.remove(handler);
            logger.debug("Unsubscribed handler for message type: {}", messageType);
        }
    }

    @Override
    public void start() {
        running = true;
        logger.info("In-memory message bus started");
    }

    @Override
    public void stop() {
        running = false;
        subscribers.clear();
        logger.info("In-memory message bus stopped");
    }
}