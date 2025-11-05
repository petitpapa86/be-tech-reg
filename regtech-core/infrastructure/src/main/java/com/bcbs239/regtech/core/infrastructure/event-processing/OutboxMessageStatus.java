package com.bcbs239.regtech.core.infrastructure.eventprocessing;

/**
 * Status enum for outbox messages.
 */
public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    PUBLISHED,  // Successfully published to event bus
    DEAD_LETTER // Moved to dead letter after exhausting retries
}
