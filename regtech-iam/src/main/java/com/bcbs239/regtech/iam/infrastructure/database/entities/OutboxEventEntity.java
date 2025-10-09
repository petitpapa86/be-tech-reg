package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for outbox_events table implementing the transactional outbox pattern.
 * Stores events to be published reliably after successful business transactions.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_events_status", columnList = "status"),
    @Index(name = "idx_outbox_events_created_at", columnList = "created_at"),
    @Index(name = "idx_outbox_events_event_type", columnList = "event_type")
})
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private String id;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 255)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Default constructor for JPA
    protected OutboxEventEntity() {}

    // Constructor for creation
    public OutboxEventEntity(String eventType, String aggregateType, String aggregateId,
                           String eventData, Instant createdAt) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventData = eventData;
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = createdAt;
        this.retryCount = 0;
        this.version = 0L;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public OutboxEventStatus getStatus() { return status; }
    public void setStatus(OutboxEventStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Mark the event as processed
     */
    public void markAsProcessed() {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    /**
     * Mark the event as failed with error message
     */
    public void markAsFailed(String error) {
        this.status = OutboxEventStatus.FAILED;
        this.lastError = error;
        this.retryCount++;
    }

    /**
     * Check if the event can be retried
     */
    public boolean canRetry() {
        return retryCount < 3; // Max 3 retries
    }

    /**
     * Reset to pending for retry
     */
    public void resetForRetry() {
        this.status = OutboxEventStatus.PENDING;
    }

    /**
     * Outbox event status enumeration
     */
    public enum OutboxEventStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }
}