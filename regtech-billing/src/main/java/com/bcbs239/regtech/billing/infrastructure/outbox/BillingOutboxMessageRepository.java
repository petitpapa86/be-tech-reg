package com.bcbs239.regtech.billing.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing outbox messages in the billing context.
 * Provides queries for finding unprocessed messages and managing the outbox.
 */
@Repository
public interface BillingOutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Find all unprocessed messages ordered by creation time.
     */
    @Query("SELECT m FROM OutboxMessage m WHERE m.processedAt IS NULL ORDER BY m.createdAt ASC")
    List<OutboxMessage> findUnprocessedMessages();

    /**
     * Find messages that should be retried (not processed and retry count < 3).
     */
    @Query("SELECT m FROM OutboxMessage m WHERE m.processedAt IS NULL AND m.retryCount < 3 ORDER BY m.createdAt ASC")
    List<OutboxMessage> findMessagesForRetry();

    /**
     * Find messages older than the specified time that are still unprocessed.
     */
    @Query("SELECT m FROM OutboxMessage m WHERE m.processedAt IS NULL AND m.createdAt < :cutoffTime ORDER BY m.createdAt ASC")
    List<OutboxMessage> findStaleMessages(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count unprocessed messages.
     */
    @Query("SELECT COUNT(m) FROM OutboxMessage m WHERE m.processedAt IS NULL")
    long countUnprocessedMessages();

    /**
     * Find messages by correlation ID.
     */
    List<OutboxMessage> findByCorrelationId(String correlationId);

    /**
     * Find messages by event type.
     */
    List<OutboxMessage> findByEventType(String eventType);
}