package com.bcbs239.regtech.core.domain.eventprocessing;

import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.List;

/**
 * Repository interface for event processing failures.
 * Handles persistence of failed event processing attempts with retry logic.
 */
public interface IEventProcessingFailureRepository {

    /**
     * Save a failed event processing attempt
     */
    Result<EventProcessingFailure> save(EventProcessingFailure failure);

    /**
     * Find events that are ready for retry (pending status and next_retry_at <= now)
     */
    Result<List<EventProcessingFailure>> findEventsReadyForRetry(int batchSize);

    /**
     * Find failed events by user ID
     */
    Result<List<EventProcessingFailure>> findByUserId(String userId);

    /**
     * Find failed event by ID
     */
    Result<EventProcessingFailure> findById(String id);

    /**
     * Delete permanently failed events (for cleanup)
     */
    Result<Integer> deletePermanentlyFailedEvents(int daysOld);
}