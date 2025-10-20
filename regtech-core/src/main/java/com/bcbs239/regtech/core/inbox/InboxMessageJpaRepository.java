package com.bcbs239.regtech.core.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * JPA repository for inbox message persistence operations.
 * Provides CRUD operations and custom queries for inbox message processing.
 */
@Repository
public interface InboxMessageJpaRepository extends JpaRepository<InboxMessageEntity, String> {

    /**
     * Find pending messages ordered by received time (oldest first)
     */
    List<InboxMessageEntity> findByProcessingStatusOrderByReceivedAtAsc(
        InboxMessageEntity.ProcessingStatus status
    );

    /**
     * Find pending messages with limit
     */
    @Query("SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC")
    List<InboxMessageEntity> findPendingMessages(
        @Param("status") InboxMessageEntity.ProcessingStatus status
    );

    /**
     * Find failed messages eligible for retry
     */
    @Query("SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = 'FAILED' AND i.nextRetryAt <= :now ORDER BY i.nextRetryAt ASC")
    List<InboxMessageEntity> findFailedMessagesEligibleForRetry(@Param("now") Instant now);

    /**
     * Mark message as processing
     */
    @Modifying
    @Query("UPDATE InboxMessageEntity i SET i.processingStatus = 'PROCESSING' WHERE i.id = :id AND i.processingStatus = 'PENDING'")
    int markAsProcessing(@Param("id") String id);

    /**
     * Mark message as processed
     */
    @Modifying
    @Query("UPDATE InboxMessageEntity i SET i.processingStatus = 'PROCESSED', i.processedAt = :processedAt WHERE i.id = :id")
    int markAsProcessed(@Param("id") String id, @Param("processedAt") Instant processedAt);

    /**
     * Mark message as failed with retry
     */
    @Modifying
    @Query("UPDATE InboxMessageEntity i SET i.processingStatus = 'FAILED', i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1, i.nextRetryAt = :nextRetryAt WHERE i.id = :id")
    int markAsFailedWithRetry(@Param("id") String id, @Param("errorMessage") String errorMessage, @Param("nextRetryAt") Instant nextRetryAt);

    /**
     * Mark message as permanently failed
     */
    @Modifying
    @Query("UPDATE InboxMessageEntity i SET i.processingStatus = 'FAILED', i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1 WHERE i.id = :id")
    int markAsPermanentlyFailed(@Param("id") String id, @Param("errorMessage") String errorMessage);

    /**
     * Count messages by status
     */
    long countByProcessingStatus(InboxMessageEntity.ProcessingStatus status);

    /**
     * Delete processed messages older than specified date (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM InboxMessageEntity i WHERE i.processingStatus = 'PROCESSED' AND i.processedAt < :cutoffDate")
    int deleteProcessedMessagesOlderThan(@Param("cutoffDate") Instant cutoffDate);
}