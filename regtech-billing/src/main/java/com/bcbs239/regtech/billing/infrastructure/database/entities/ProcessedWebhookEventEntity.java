package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.valueobjects.ProcessedWebhookEvent;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for ProcessedWebhookEvent value object persistence.
 * Maps domain value object to database table structure for idempotency tracking.
 */
@Entity
@Table(name = "processed_webhook_events")
public class ProcessedWebhookEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 50)
    private ProcessedWebhookEvent.ProcessingResult result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Default constructor for JPA
    public ProcessedWebhookEventEntity() {}

    /**
     * Convert domain value object to JPA entity
     */
    public static ProcessedWebhookEventEntity fromDomain(ProcessedWebhookEvent event) {
        ProcessedWebhookEventEntity entity = new ProcessedWebhookEventEntity();
        
        entity.id = UUID.randomUUID().toString(); // Generate ID for entity
        entity.stripeEventId = event.eventId();
        entity.eventType = event.eventType();
        entity.processedAt = event.processedAt();
        entity.result = event.result();
        entity.errorMessage = event.errorMessage();
        
        return entity;
    }

    /**
     * Convert JPA entity to domain value object
     */
    public ProcessedWebhookEvent toDomain() {
        return new ProcessedWebhookEvent(
            this.stripeEventId,
            this.eventType,
            this.result,
            this.errorMessage,
            this.processedAt
        );
    }

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStripeEventId() { return stripeEventId; }
    public void setStripeEventId(String stripeEventId) { this.stripeEventId = stripeEventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public ProcessedWebhookEvent.ProcessingResult getResult() { return result; }
    public void setResult(ProcessedWebhookEvent.ProcessingResult result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
