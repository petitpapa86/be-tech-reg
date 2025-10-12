package com.bcbs239.regtech.core.saga;

/**
 * Common interface for messages exchanged between bounded contexts.
 * Provides basic metadata for routing and correlation.
 */
public interface Message {

    /**
     * The message type for routing (e.g., "UserRegistered", "PaymentProcessed")
     */
    String getType();

    /**
     * Optional correlation ID for tracking message flows
     */
    default String getCorrelationId() {
        return null;
    }

    /**
     * Optional message ID for unique identification
     */
    default String getMessageId() {
        return null;
    }

    /**
     * Optional source context identifier
     */
    default String getSource() {
        return null;
    }

    /**
     * Optional target context identifier
     */
    default String getTarget() {
        return null;
    }
}