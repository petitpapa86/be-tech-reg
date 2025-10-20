package com.bcbs239.regtech.core.inbox;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Shared inbox message entity for reliable event processing across bounded contexts.
 * This entity stores integration events that need to be processed by event handlers.
 */
@Entity
@Table(name = "inbox_messages", indexes = {
    @Index(name = "idx_inbox_messages_status_received", columnList = "processing_status, received_at"),
    @Index(name = "idx_inbox_messages_event_type", columnList = "event_type"),
    @Index(name = "idx_inbox_messages_aggregate_id", columnList = "aggregate_id")
})
public class InboxMessageEntity {

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    // Default constructor
    public InboxMessageEntity() {}

    // Constructor for creating new inbox messages
    public InboxMessageEntity(String eventType, String eventData, String aggregateId) {
        this.eventType = eventType;
        this.eventData = eventData;
        this.aggregateId = aggregateId;
        this.receivedAt = Instant.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}