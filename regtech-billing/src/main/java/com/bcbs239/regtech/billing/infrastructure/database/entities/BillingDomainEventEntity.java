package com.bcbs239.regtech.billing.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Entity for storing domain events in the outbox pattern.
 * Ensures reliable event delivery by persisting events in the same transaction as business data.
 */
@Getter
@Entity
@Table(name = "billing_domain_events", schema = "billing")
public class BillingDomainEventEntity {

    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "source_module", nullable = false)
    private String sourceModule;

    @Column(name = "target_module")
    private String targetModule;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Version
    private Long version;

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED,
        DEAD_LETTER
    }

    // Default constructor for JPA
    protected BillingDomainEventEntity() {}

    public BillingDomainEventEntity(String eventId, String eventType, String correlationId, 
                                  String sourceModule, String targetModule, String eventData) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.sourceModule = sourceModule;
        this.targetModule = targetModule;
        this.eventData = eventData;
        this.createdAt = Instant.now();
        this.processingStatus = ProcessingStatus.PENDING;
        this.retryCount = 0;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Business methods
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

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries && processingStatus == ProcessingStatus.FAILED;
    }

    @Override
    public String toString() {
        return String.format("BillingDomainEventEntity{id=%d, eventId='%s', eventType='%s', status=%s, retryCount=%d}", 
            id, eventId, eventType, processingStatus, retryCount);
    }
}
