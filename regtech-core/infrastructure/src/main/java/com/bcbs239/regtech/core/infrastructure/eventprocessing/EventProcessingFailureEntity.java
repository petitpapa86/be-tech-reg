package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for event processing failures.
 * Stores failed event processing attempts for retry with exponential backoff.
 */
@Setter
@Getter
@Entity
@Table(name = "event_processing_failures", indexes = {
    @Index(name = "idx_event_failures_retry", columnList = "next_retry_at, status"),
    @Index(name = "idx_event_failures_status", columnList = "status")
})
public class EventProcessingFailureEntity {

    @Id
    private String id;

    @Column(name = "event_type", nullable = false, length = 500)
    private String eventType;

    @Column(name = "event_payload", nullable = false)
    private String eventPayload;

    // Domain-specific metadata stored as JSON string. Contains optional keys like "userId" and "billingAccountId".
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stacktrace", columnDefinition = "TEXT")
    private String errorStacktrace;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventProcessingStatus status = EventProcessingStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;
}