package com.bcbs239.regtech.core.inbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a consumer that has processed an inbox message.
 * Used to ensure idempotent processing of integration events.
 */
@Entity
@Table(name = "inbox_message_consumers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inbox_message_id", "name"}))
public class InboxMessageConsumer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbox_message_id", nullable = false)
    private String inboxMessageId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected InboxMessageConsumer() {
        // JPA default constructor
    }

    public InboxMessageConsumer(String inboxMessageId, String name) {
        this.inboxMessageId = inboxMessageId;
        this.name = name;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getInboxMessageId() {
        return inboxMessageId;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}