package com.bcbs239.regtech.core.outbox;

import java.time.Instant;

/**
 * Generic representation of an outbox message row loaded from DB
 */
public class OutboxMessage {
    private String id;
    private String eventType;
    private String payload; // JSON
    private Instant createdAt;
    private String aggregateId;

    public OutboxMessage(String id, String eventType, String payload, Instant createdAt, String aggregateId) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.aggregateId = aggregateId;
    }

    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAggregateId() { return aggregateId; }
}
