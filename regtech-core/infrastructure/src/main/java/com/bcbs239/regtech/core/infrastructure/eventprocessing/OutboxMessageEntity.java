package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.outbox.OutboxMessageStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA Entity for outbox_messages table implementing the transactional outbox pattern.
 * Stores events to be published reliably after successful business transactions.
 */
@Setter
@Getter
@Entity
@Table(name = "outbox_messages", indexes = {
    @Index(name = "idx_outbox_messages_status", columnList = "status"),
    @Index(name = "idx_outbox_messages_occurred_on_utc", columnList = "occurred_on_utc"),
    @Index(name = "idx_outbox_messages_type", columnList = "type")
})
public class OutboxMessageEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private String id;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OutboxMessageStatus status;

    @Column(name = "occurred_on_utc", nullable = false)
    private Instant occurredOnUtc;

    @Column(name = "processed_on_utc")
    private Instant processedOnUtc;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_time")
    private Instant nextRetryTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "dead_letter_time")
    private Instant deadLetterTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public OutboxMessageEntity() {}

    public OutboxMessageEntity(String type, String content, Instant occurredOnUtc) {
        this.type = type;
        this.content = content;
        this.occurredOnUtc = occurredOnUtc;
        this.status = OutboxMessageStatus.PENDING;
        this.updatedAt = Instant.now();
    }

}
