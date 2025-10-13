package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for storing incoming cross-module events in the inbox pattern.
 * Events are received from other bounded contexts and processed asynchronously.
 */
@Entity(name = "iamInboxEventEntity")
@Table(name = "iam_inbox_events")
public class InboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    // Default constructor for JPA
    protected InboxEventEntity() {}

    public InboxEventEntity(String eventType, String aggregateId, String eventData) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.eventData = eventData;
        this.receivedAt = Instant.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventData() { return eventData; }
    public Instant getReceivedAt() { return receivedAt; }
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public Instant getProcessedAt() { return processedAt; }
    public String getLastError() { return lastError; }
    public int getRetryCount() { return retryCount; }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
        if (processingStatus == ProcessingStatus.PROCESSED) {
            this.processedAt = Instant.now();
        }
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
        this.retryCount++;
    }

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries;
    }

    public void markAsProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    public void markAsProcessed() {
        this.processingStatus = ProcessingStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markAsFailed(String error) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.lastError = error;
        this.retryCount++;
    }

    public void markAsDeadLetter() {
        this.processingStatus = ProcessingStatus.DEAD_LETTER;
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED,
        DEAD_LETTER
    }
}