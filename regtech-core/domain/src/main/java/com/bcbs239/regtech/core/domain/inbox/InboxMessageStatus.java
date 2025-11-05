package com.bcbs239.regtech.core.domain.inbox;

public enum InboxMessageStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    PUBLISHED,  // Successfully published to event bus
    DEAD_LETTER // Moved to dead letter after exhausting retries
}
