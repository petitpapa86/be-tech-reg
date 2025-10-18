package com.bcbs239.regtech.core.infrastructure.outbox;

/**
 * Status enum for outbox messages.
 */
public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}