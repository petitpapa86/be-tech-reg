package com.bcbs239.regtech.core.domain.inbox;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class InboxMessage {
    private String id;

    private String eventType;

    private String content;

    private InboxMessageStatus status;

    private Instant occurredOnUtc;

    private Instant processedOnUtc;

    private int retryCount = 0;

    private Instant nextRetryTime;

    private String lastError;

    private Instant deadLetterTime;

    private Instant updatedAt;
}

