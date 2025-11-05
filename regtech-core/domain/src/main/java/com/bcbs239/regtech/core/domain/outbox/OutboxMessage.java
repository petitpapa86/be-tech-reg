package com.bcbs239.regtech.core.domain.eventprocessing;

import com.bcbs239.regtech.core.domain.events.OutboxMessageStatus;

import java.time.Instant;

/**
 * Domain interface for outbox messages implementing the transactional outbox pattern.
 * Stores events to be published reliably after successful business transactions.
 */
public interface OutboxMessage {

    String getId();
    String getType();
    String getContent();
    OutboxMessageStatus getStatus();
    Instant getOccurredOnUtc();
    Instant getProcessedOnUtc();
    int getRetryCount();
    Instant getNextRetryTime();
    String getLastError();
    Instant getDeadLetterTime();

    // Business methods
    boolean isPending();
    boolean isProcessed();
    boolean isFailed();
    boolean canRetry();
    void markAsProcessing();
    void markAsProcessed();
    void markAsFailed(String error);
    void incrementRetryCount();
}