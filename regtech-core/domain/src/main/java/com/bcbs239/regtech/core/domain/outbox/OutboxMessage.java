package com.bcbs239.regtech.core.domain.outbox;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class OutboxMessage {
    private String id;

    private String type;

    private String content;

    private OutboxMessageStatus status;

    private Instant occurredOnUtc;

    private Instant processedOnUtc;

    private int retryCount = 0;

    private Instant nextRetryTime;

    private String lastError;

    private Instant deadLetterTime;

    private Instant updatedAt;

    private String  errorMessage;

    public OutboxMessage(String type, String content, Instant now) {
        this.type = type;
        this.content = content;
        this.status = OutboxMessageStatus.PENDING;
        this.occurredOnUtc = now;
        this.updatedAt = now;
    }
}

