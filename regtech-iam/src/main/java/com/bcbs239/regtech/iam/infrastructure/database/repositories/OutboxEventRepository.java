package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.infrastructure.database.entities.OutboxEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Repository for managing outbox events in the transactional outbox pattern.
 */

@Repository
public class OutboxEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventRepository.class);

    /**
     * Closure to save an outbox event
     * Returns Result<String> for functional error handling
     */
    public Function<OutboxEventEntity, Result<String>> eventSaver() {
        return event -> {
            try {
                logger.debug("========================>Creating outbox event saver closure");
                if (event.getId() == null) {
                    entityManager.persist(event);
                } else {
                    event = entityManager.merge(event);
                }

                entityManager.flush();
                return Result.success(event.getId());
            } catch (Exception e) {
                // Rethrow to ensure the transaction is marked for rollback immediately and
                // the exception can be handled by the controller or a global exception handler.
                throw new RuntimeException("OUTBOX_EVENT_SAVE_FAILED: Failed to save outbox event: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Find pending outbox events for processing
     */
    public List<OutboxEventEntity> findPendingEvents(int limit) {
        return entityManager.createQuery(
            "SELECT e FROM OutboxEventEntity e WHERE e.status = :status ORDER BY e.createdAt ASC",
            OutboxEventEntity.class)
            .setParameter("status", OutboxEventEntity.OutboxEventStatus.PENDING)
            .setMaxResults(limit)
            .getResultList();
    }

    /**
     * Update an outbox event status
     */
    public Result<Void> updateEventStatus(String eventId, OutboxEventEntity.OutboxEventStatus status,
                                        Instant processedAt, String error) {
        try {
            OutboxEventEntity event = entityManager.find(OutboxEventEntity.class, eventId);
            if (event == null) {
                return Result.failure(ErrorDetail.of("OUTBOX_EVENT_NOT_FOUND",
                    "Outbox event not found: " + eventId, "error.outbox.notFound"));
            }

            event.setStatus(status);
            if (processedAt != null) {
                event.setProcessedAt(processedAt);
            }
            if (error != null) {
                event.setLastError(error);
                event.setRetryCount(event.getRetryCount() + 1);
            }

            entityManager.merge(event);
            entityManager.flush();
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("OUTBOX_EVENT_UPDATE_FAILED",
                "Failed to update outbox event: " + e.getMessage(), "error.outbox.updateFailed"));
        }
    }

    /**
     * Mark event as processing
     */
    public Result<Void> markAsProcessing(String eventId) {
        return updateEventStatus(eventId, OutboxEventEntity.OutboxEventStatus.PROCESSING, null, null);
    }

    /**
     * Mark event as processed
     */
    public Result<Void> markAsProcessed(String eventId) {
        return updateEventStatus(eventId, OutboxEventEntity.OutboxEventStatus.PROCESSED, Instant.now(), null);
    }

    /**
     * Mark event as failed
     */
    public Result<Void> markAsFailed(String eventId, String error) {
        return updateEventStatus(eventId, OutboxEventEntity.OutboxEventStatus.FAILED, null, error);
    }
}