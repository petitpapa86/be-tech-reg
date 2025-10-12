package com.bcbs239.regtech.core.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * In-memory message bus implementation for modular monolith.
 * Routes messages between bounded contexts within the same JVM.
 */
@Component
public class InMemoryMessageBus implements MessageBus<Message> {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryMessageBus.class);

    private final Map<String, List<MessageHandler<Message>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Message>> pendingResponses = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    @Override
    public void publish(Message message) {
        if (!running) {
            logger.warn("Message bus is not running, cannot publish message: {}", message);
            return;
        }

        logger.debug("Publishing message: {}", message);
        List<MessageHandler<Message>> handlers = subscribers.get(message.getType());
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
    public Message sendAndReceive(Message message) {
        if (!running) {
            throw new IllegalStateException("Message bus is not running");
        }

        String correlationId = message.getCorrelationId() != null ?
            message.getCorrelationId() : message.getMessageId();

        if (correlationId == null) {
            throw new IllegalArgumentException("Message must have either correlationId or messageId for request-reply");
        }

        CompletableFuture<Message> responseFuture = new CompletableFuture<>();
        pendingResponses.put(correlationId, responseFuture);

        try {
            // Publish the request message
            publish(message);

            // Wait for response with timeout
            return responseFuture.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingResponses.remove(correlationId);
            throw new RuntimeException("Timeout waiting for response to message: " + message, e);
        } catch (Exception e) {
            pendingResponses.remove(correlationId);
            throw new RuntimeException("Error in request-reply for message: " + message, e);
        } finally {
            // Clean up the pending response
            pendingResponses.remove(correlationId);
        }
    }

    /**
     * Sends a response message for a request-reply pattern.
     * The response should have the same correlationId as the original request.
     */
    public void sendResponse(Message response) {
        if (!running) {
            logger.warn("Message bus is not running, cannot send response: {}", response);
            return;
        }

        String correlationId = response.getCorrelationId();
        if (correlationId == null) {
            logger.warn("Response message has no correlationId, cannot match to pending request: {}", response);
            return;
        }

        CompletableFuture<Message> pendingResponse = pendingResponses.get(correlationId);
        if (pendingResponse != null) {
            pendingResponse.complete(response);
            logger.debug("Sent response for correlationId: {}", correlationId);
        } else {
            logger.warn("No pending request found for correlationId: {}, response will be ignored", correlationId);
        }
    }

    @Override
    public void subscribe(String messageType, MessageHandler<Message> handler) {
        subscribers.computeIfAbsent(messageType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("Subscribed handler for message type: {}", messageType);
    }

    @Override
    public void unsubscribe(String messageType, MessageHandler<Message> handler) {
        List<MessageHandler<Message>> handlers = subscribers.get(messageType);
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

        // Cancel all pending responses
        pendingResponses.values().forEach(future -> future.cancel(true));
        pendingResponses.clear();

        logger.info("In-memory message bus stopped");
    }
}