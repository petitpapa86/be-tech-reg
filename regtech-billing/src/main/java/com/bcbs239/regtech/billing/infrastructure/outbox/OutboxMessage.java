package com.bcbs239.regtech.billing.infrastructure.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox message entity for reliable event publishing in the billing context.
 * Stores events that need to be published to other bounded contexts.
 */
@Entity
@Table(name = "billing_outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private String sourceModule = "billing";

    @Column
    private Integer retryCount = 0;

    @Column
    private String errorMessage;

    @Version
    private Long version;

    // Constructors
    protected OutboxMessage() {}

    public OutboxMessage(String eventType, String payload, String correlationId) {
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public String getCorrelationId() { return correlationId; }
    public String getSourceModule() { return sourceModule; }
    public Integer getRetryCount() { return retryCount; }
    public String getErrorMessage() { return errorMessage; }
    public Long getVersion() { return version; }

    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * Mark this message as processed successfully.
     */
    public void markAsProcessed() {
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Increment retry count and set error message.
     */
    public void incrementRetry(String errorMessage) {
        this.retryCount = this.retryCount + 1;
        this.errorMessage = errorMessage;
    }

    /**
     * Check if message is processed.
     */
    public boolean isProcessed() {
        return processedAt != null;
    }

    /**
     * Check if message should be retried (max 3 attempts).
     */
    public boolean shouldRetry() {
        return !isProcessed() && retryCount < 3;
    }
}