package com.bcbs239.regtech.core.domain.eventprocessing;

import java.time.Instant;

/**
 * Domain interface for inbox messages for reliable event processing across bounded contexts.
 * Stores integration events that need to be processed by event handlers.
 */
public interface InboxMessage {

    enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }

    String getId();
    String getEventId();
    String getEventType();
    String getEventData();
    Instant getReceivedAt();
    Instant getProcessedAt();
    ProcessingStatus getProcessingStatus();

    // Business methods
    boolean isPending();
    boolean isProcessing();
    boolean isProcessed();
    boolean isFailed();
    void markAsProcessing();
    void markAsProcessed();
    void markAsFailed();
}