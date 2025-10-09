package com.bcbs239.regtech.billing.infrastructure.database.repositories;

import com.bcbs239.regtech.billing.domain.valueobjects.ProcessedWebhookEvent;
import com.bcbs239.regtech.billing.infrastructure.database.entities.ProcessedWebhookEventEntity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA repository for ProcessedWebhookEvent using closure-based functional patterns.
 * Provides functional operations for webhook event idempotency tracking.
 */
@Repository
@Transactional
public class JpaProcessedWebhookEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Function<String, Maybe<ProcessedWebhookEvent>> processedWebhookEventFinder() {
        return stripeEventId -> {
            try {
                ProcessedWebhookEventEntity entity = entityManager.createQuery(
                    "SELECT pwe FROM ProcessedWebhookEventEntity pwe WHERE pwe.stripeEventId = :stripeEventId", 
                    ProcessedWebhookEventEntity.class)
                    .setParameter("stripeEventId", stripeEventId)
                    .getSingleResult();
                return Maybe.some(entity.toDomain());
            } catch (NoResultException e) {
                return Maybe.none();
            } catch (Exception e) {
                // Log error but return none for functional pattern
                return Maybe.none();
            }
        };
    }

    public Function<ProcessedWebhookEvent, Result<String>> processedWebhookEventSaver() {
        return event -> {
            try {
                ProcessedWebhookEventEntity entity = ProcessedWebhookEventEntity.fromDomain(event);
                entityManager.persist(entity);
                entityManager.flush(); // Ensure the entity is persisted
                
                return Result.success(entity.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("PROCESSED_WEBHOOK_EVENT_SAVE_FAILED",
                    "Failed to save processed webhook event: " + e.getMessage(), "webhook.event.save.failed"));
            }
        };
    }

    public Function<String, List<ProcessedWebhookEvent>> processedWebhookEventsByTypeFinder() {
        return eventType -> {
            try {
                List<ProcessedWebhookEventEntity> entities = entityManager.createQuery(
                    "SELECT pwe FROM ProcessedWebhookEventEntity pwe WHERE pwe.eventType = :eventType ORDER BY pwe.processedAt DESC", 
                    ProcessedWebhookEventEntity.class)
                    .setParameter("eventType", eventType)
                    .getResultList();
                
                return entities.stream()
                    .map(ProcessedWebhookEventEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    public Function<ProcessedWebhookEvent.ProcessingResult, List<ProcessedWebhookEvent>> processedWebhookEventsByResultFinder() {
        return result -> {
            try {
                List<ProcessedWebhookEventEntity> entities = entityManager.createQuery(
                    "SELECT pwe FROM ProcessedWebhookEventEntity pwe WHERE pwe.result = :result ORDER BY pwe.processedAt DESC", 
                    ProcessedWebhookEventEntity.class)
                    .setParameter("result", result)
                    .getResultList();
                
                return entities.stream()
                    .map(ProcessedWebhookEventEntity::toDomain)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error but return empty list for functional pattern
                return List.of();
            }
        };
    }

    /**
     * Check if a webhook event has already been processed (for idempotency)
     */
    public boolean isEventProcessed(String stripeEventId) {
        try {
            Long count = entityManager.createQuery(
                "SELECT COUNT(pwe) FROM ProcessedWebhookEventEntity pwe WHERE pwe.stripeEventId = :stripeEventId", 
                Long.class)
                .setParameter("stripeEventId", stripeEventId)
                .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            // Log error but return false to allow processing
            return false;
        }
    }

    /**
     * Clean up old processed webhook events (for maintenance)
     */
    public int cleanupOldEvents(int daysToKeep) {
        try {
            Instant cutoffDate = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
            return entityManager.createQuery(
                "DELETE FROM ProcessedWebhookEventEntity pwe WHERE pwe.processedAt < :cutoffDate")
                .setParameter("cutoffDate", cutoffDate)
                .executeUpdate();
        } catch (Exception e) {
            // Log error but return 0
            return 0;
        }
    }

    /**
     * Get recent failed webhook events for monitoring
     */
    public List<ProcessedWebhookEvent> getRecentFailedEvents(int hours) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            List<ProcessedWebhookEventEntity> entities = entityManager.createQuery(
                "SELECT pwe FROM ProcessedWebhookEventEntity pwe WHERE pwe.result = :result AND pwe.processedAt >= :since ORDER BY pwe.processedAt DESC", 
                ProcessedWebhookEventEntity.class)
                .setParameter("result", ProcessedWebhookEvent.ProcessingResult.FAILURE)
                .setParameter("since", since)
                .getResultList();
            
            return entities.stream()
                .map(ProcessedWebhookEventEntity::toDomain)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error but return empty list for functional pattern
            return List.of();
        }
    }
}
