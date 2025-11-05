package com.bcbs239.regtech.core.domain.eventprocessing;

import java.time.LocalDateTime;

/**
 * Domain interface for consumers that have processed inbox messages.
 * Used to ensure idempotent processing of integration events.
 */
public interface InboxMessageConsumer {

    Long getId();
    String getInboxMessageId();
    String getName();
    LocalDateTime getProcessedAt();
}