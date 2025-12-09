package com.bcbs239.regtech.billing.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA Entity for Saga Audit Log persistence.
 * Tracks saga execution events for compliance and monitoring.
 */
@Setter
@Getter
@Entity
@Table(name = "saga_audit_log", schema = "billing")
public class SagaAuditLogEntity {

    // Getters and setters for JPA
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

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

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "billing_account_id")
    private String billingAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public SagaAuditLogEntity() {}

    /**
     * Constructor for creating audit log entries
     */
    public SagaAuditLogEntity(String id, String sagaId, String sagaType, String eventType, 
                             String eventData, String userId, String billingAccountId) {
        this.id = id;
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.userId = userId;
        this.billingAccountId = billingAccountId;
        this.createdAt = Instant.now();
    }

}

