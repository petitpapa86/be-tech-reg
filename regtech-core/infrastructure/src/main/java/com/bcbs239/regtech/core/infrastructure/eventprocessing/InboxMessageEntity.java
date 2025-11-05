package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Shared inbox message entity for reliable event processing across bounded contexts.
 * This entity stores integration events that need to be processed by event handlers.
 */
@Setter
@Getter
@Entity
@Table(name = "inbox_messages", indexes = {
    @Index(name = "idx_inbox_messages_status_received", columnList = "processing_status, received_at"),
    @Index(name = "idx_inbox_messages_event_type", columnList = "event_type"),
    @Index(name = "idx_inbox_messages_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_inbox_messages_event_id", columnList = "event_id")
})
public class InboxMessageEntity  {

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }

    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_id", length = 100, unique = true)
    private String eventId; // new: original integration event id for idempotency

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

}
