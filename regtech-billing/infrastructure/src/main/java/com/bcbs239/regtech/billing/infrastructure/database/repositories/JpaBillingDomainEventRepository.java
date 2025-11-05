package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.infrastructure.database.entities.BillingDomainEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for billing domain events in the outbox pattern.
 */
@Repository
public interface JpaBillingDomainEventRepository extends JpaRepository<BillingDomainEventEntity, Long> {

    /**
     * Find events that are pending processing, ordered by creation time.
     */
    @Query("SELECT e FROM BillingDomainEventEntity e WHERE e.processingStatus = 'PENDING' ORDER BY e.createdAt ASC")
    List<BillingDomainEventEntity> findPendingEvents();

    /**
     * Find events that failed and can be retried.
     */
    @Query("SELECT e FROM BillingDomainEventEntity e WHERE e.processingStatus = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<BillingDomainEventEntity> findRetryableEvents(@Param("maxRetries") int maxRetries);

    /**
     * Find events that are stuck in processing status for too long.
     */
    @Query("SELECT e FROM BillingDomainEventEntity e WHERE e.processingStatus = 'PROCESSING' AND e.createdAt < :staleThreshold")
    List<BillingDomainEventEntity> findStaleProcessingEvents(@Param("staleThreshold") Instant staleThreshold);

    /**
     * Find event by event ID for idempotency checking.
     */
    Optional<BillingDomainEventEntity> findByEventId(String eventId);

    /**
     * Find events by correlation ID for debugging and tracing.
     */
    List<BillingDomainEventEntity> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    /**
     * Find events by target module for module-specific processing.
     */
    @Query("SELECT e FROM BillingDomainEventEntity e WHERE e.targetModule = :targetModule AND e.processingStatus = 'PENDING' ORDER BY e.createdAt ASC")
    List<BillingDomainEventEntity> findPendingEventsByTargetModule(@Param("targetModule") String targetModule);

    /**
     * Count events by processing status for monitoring.
     */
    @Query("SELECT COUNT(e) FROM BillingDomainEventEntity e WHERE e.processingStatus = :status")
    long countByProcessingStatus(@Param("status") BillingDomainEventEntity.ProcessingStatus status);

    /**
     * Delete processed events older than the specified threshold for cleanup.
     */
    @Query("DELETE FROM BillingDomainEventEntity e WHERE e.processingStatus = 'PROCESSED' AND e.processedAt < :threshold")
    void deleteProcessedEventsOlderThan(@Param("threshold") Instant threshold);
}

