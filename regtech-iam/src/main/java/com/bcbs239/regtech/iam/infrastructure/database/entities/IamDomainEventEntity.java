package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for storing IAM domain events in outbox pattern.
 * Ensures reliable event delivery by persisting events in the same transaction as business data.
 */
@Entity
@Table(name = "iam_domain_events", schema = "iam")
public class IamDomainEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "source_module", nullable = false)
    private String sourceModule;

    @Column(name = "target_module")
    private String targetModule;

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    // Default constructor for JPA
    public IamDomainEventEntity() {}

    public IamDomainEventEntity(String id, String eventType, String correlationId, 
                               String sourceModule, String targetModule, String eventData) {
        this.id = id;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.sourceModule = sourceModule;
        this.targetModule = targetModule;
        this.eventData = eventData;
        this.processingStatus = ProcessingStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markAsProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    public void markAsProcessed() {
        this.processingStatus = ProcessingStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.lastError = errorMessage;
        this.failedAt = Instant.now();
        this.retryCount++;
    }

    public void markAsDeadLetter() {
        this.processingStatus = ProcessingStatus.DEAD_LETTER;
    }

    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries;
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED,
        DEAD_LETTER
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSourceModule() { return sourceModule; }
    public void setSourceModule(String sourceModule) { this.sourceModule = sourceModule; }

    public String getTargetModule() { return targetModule; }
    public void setTargetModule(String targetModule) { this.targetModule = targetModule; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
}