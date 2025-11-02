package com.bcbs239.regtech.core.infrastructure.outbox;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for outbox_messages table implementing the transactional outbox pattern.
 * Stores events to be published reliably after successful business transactions.
 */
@Entity
@Table(name = "outbox_messages", indexes = {
    @Index(name = "idx_outbox_messages_status", columnList = "status"),
    @Index(name = "idx_outbox_messages_occurred_on_utc", columnList = "occurred_on_utc"),
    @Index(name = "idx_outbox_messages_type", columnList = "type")
})
public class OutboxMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private String id;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OutboxMessageStatus status;

    @Column(name = "occurred_on_utc", nullable = false)
    private Instant occurredOnUtc;

    @Column(name = "processed_on_utc")
    private Instant processedOnUtc;
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
    
    @Column(name = "next_retry_time")
    private Instant nextRetryTime;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "dead_letter_time")
    private Instant deadLetterTime;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public OutboxMessageEntity() {}

    public OutboxMessageEntity(String type, String content, Instant occurredOnUtc) {
        this.type = type;
        this.content = content;
        this.status = OutboxMessageStatus.PENDING;
        this.occurredOnUtc = occurredOnUtc;
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public OutboxMessageStatus getStatus() { return status; }
    public void setStatus(OutboxMessageStatus status) { this.status = status; }

    public Instant getOccurredOnUtc() { return occurredOnUtc; }
    public void setOccurredOnUtc(Instant occurredOnUtc) { this.occurredOnUtc = occurredOnUtc; }

    public Instant getProcessedOnUtc() { return processedOnUtc; }
    public void setProcessedOnUtc(Instant processedOnUtc) { this.processedOnUtc = processedOnUtc; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { 
        this.retryCount = retryCount; 
        this.updatedAt = Instant.now();
    }
    
    public Instant getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(Instant nextRetryTime) { 
        this.nextRetryTime = nextRetryTime; 
        this.updatedAt = Instant.now();
    }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { 
        this.lastError = lastError; 
        this.updatedAt = Instant.now();
    }
    
    public Instant getDeadLetterTime() { return deadLetterTime; }
    public void setDeadLetterTime(Instant deadLetterTime) { 
        this.deadLetterTime = deadLetterTime; 
        this.updatedAt = Instant.now();
    }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Methods
    public void markAsProcessed() {
        this.status = OutboxMessageStatus.PROCESSED;
        this.processedOnUtc = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxMessageStatus.FAILED;
        this.lastError = errorMessage;
        this.updatedAt = Instant.now();
    }
    
    // Convenience methods for recovery service
    public String getEventType() { return type; }
    public String getEventPayload() { return content; }
    public Instant getProcessedAt() { return processedOnUtc; }
}