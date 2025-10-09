package com.bcbs239.regtech.iam.infrastructure.events;

import com.bcbs239.regtech.core.events.OutboxEventPublisher;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.OutboxEventRepository;
import com.bcbs239.regtech.iam.infrastructure.database.entities.OutboxEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * IAM event publisher implementing the transactional outbox pattern.
 * Processes outbox events and publishes them to the cross-module event bus.
 */
@Service
@Transactional
public class IamEventPublisher implements OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(IamEventPublisher.class);
    private static final int BATCH_SIZE = 10;

    @PersistenceContext
    private EntityManager entityManager;

    private final OutboxEventRepository outboxEventRepository;
    private final CrossModuleEventBus crossModuleEventBus;
    private final ObjectMapper objectMapper;

    public IamEventPublisher(OutboxEventRepository outboxEventRepository,
                           CrossModuleEventBus crossModuleEventBus,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.crossModuleEventBus = crossModuleEventBus;
        this.objectMapper = objectMapper;
    }

    /**
     * Process pending outbox events using transactional outbox pattern
     */
    @Override
    @Transactional
    public void processPendingEvents() {
        processPendingEventsWithClosures(
            () -> outboxEventRepository.findPendingEvents(BATCH_SIZE),
            eventEntity -> this.deserializeEvent(eventEntity),
            domainEvent -> this.publishEvent(domainEvent),
            statusUpdate -> this.updateEventStatus(statusUpdate)
        );
    }

    /**
     * Pure function for processing pending events with closures
     */
    static void processPendingEventsWithClosures(
            Supplier<List<OutboxEventEntity>> pendingEventsFinder,
            Function<OutboxEventEntity, Result<Object>> eventDeserializer,
            Function<Object, Result<Void>> eventPublisher,
            Function<EventStatusUpdate, Result<Void>> eventUpdater) {

        List<OutboxEventEntity> pendingEvents = pendingEventsFinder.get();

        for (OutboxEventEntity eventEntity : pendingEvents) {
            try {
                // Mark as processing
                eventUpdater.apply(new EventStatusUpdate(eventEntity.getId(),
                    OutboxEventEntity.OutboxEventStatus.PROCESSING, null, null));

                // Deserialize event
                Result<Object> deserializationResult = eventDeserializer.apply(eventEntity);
                if (deserializationResult.isFailure()) {
                    eventUpdater.apply(new EventStatusUpdate(eventEntity.getId(),
                        OutboxEventEntity.OutboxEventStatus.FAILED,
                        "Deserialization failed: " + deserializationResult.getError().get().getMessage(), null));
                    continue;
                }

                // Publish event
                Object event = deserializationResult.getValue().get();
                Result<Void> publishResult = eventPublisher.apply(event);
                if (publishResult.isFailure()) {
                    eventUpdater.apply(new EventStatusUpdate(eventEntity.getId(),
                        OutboxEventEntity.OutboxEventStatus.FAILED,
                        "Publishing failed: " + publishResult.getError().get().getMessage(), null));
                    continue;
                }

                // Mark as processed
                eventUpdater.apply(new EventStatusUpdate(eventEntity.getId(),
                    OutboxEventEntity.OutboxEventStatus.PROCESSED, null, Instant.now()));

            } catch (Exception e) {
                logger.error("Error processing outbox event {}: {}", eventEntity.getId(), e.getMessage(), e);
                try {
                    eventUpdater.apply(new EventStatusUpdate(eventEntity.getId(),
                        OutboxEventEntity.OutboxEventStatus.FAILED, e.getMessage(), null));
                } catch (Exception updateException) {
                    logger.error("Failed to update event status for {}: {}", eventEntity.getId(), updateException.getMessage());
                }
            }
        }
    }

    /**
     * Retry failed events that can be retried
     */
    @Override
    @Transactional
    public void retryFailedEvents(int maxRetries) {
        // Find failed events that haven't exceeded max retries
        List<OutboxEventEntity> failedEvents = entityManager.createQuery(
            "SELECT e FROM OutboxEventEntity e WHERE e.status = :status AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC",
            OutboxEventEntity.class)
            .setParameter("status", OutboxEventEntity.OutboxEventStatus.FAILED)
            .setParameter("maxRetries", maxRetries)
            .setMaxResults(BATCH_SIZE)
            .getResultList();

        // Reset status to PENDING for retry
        for (OutboxEventEntity event : failedEvents) {
            event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
            entityManager.merge(event);
        }

        // Process them as pending events
        processPendingEvents();
    }

    /**
     * Get statistics about event processing
     */
    @Override
    public OutboxEventStats getStats() {
        long pending = countEventsByStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        long processing = countEventsByStatus(OutboxEventEntity.OutboxEventStatus.PROCESSING);
        long processed = countEventsByStatus(OutboxEventEntity.OutboxEventStatus.PROCESSED);
        long failed = countEventsByStatus(OutboxEventEntity.OutboxEventStatus.FAILED);

        return new OutboxEventStats(pending, processing, processed, failed, 0); // No dead letter in this implementation
    }

    private long countEventsByStatus(OutboxEventEntity.OutboxEventStatus status) {
        return entityManager.createQuery(
            "SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = :status", Long.class)
            .setParameter("status", status)
            .getSingleResult();
    }

    /**
     * Deserialize event from JSON
     */
    private Result<Object> deserializeEvent(OutboxEventEntity eventEntity) {
        try {
            if ("UserRegisteredEvent".equals(eventEntity.getEventType())) {
                UserRegisteredEvent event = objectMapper.readValue(eventEntity.getEventData(), UserRegisteredEvent.class);
                return Result.success(event);
            } else {
                return Result.failure(ErrorDetail.of("UNKNOWN_EVENT_TYPE",
                    "Unknown event type: " + eventEntity.getEventType(), "event.unknown.type"));
            }
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("DESERIALIZATION_FAILED",
                "Failed to deserialize event: " + e.getMessage(), "event.deserialization.failed"));
        }
    }

    /**
     * Publish event to cross-module event bus
     */
    private Result<Void> publishEvent(Object event) {
        try {
            crossModuleEventBus.publishEvent(event);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("EVENT_PUBLISHING_FAILED",
                "Failed to publish event: " + e.getMessage(), "event.publishing.failed"));
        }
    }

    /**
     * Update event status in database
     */
    private Result<Void> updateEventStatus(EventStatusUpdate update) {
        try {
            OutboxEventEntity event = entityManager.find(OutboxEventEntity.class, update.eventId());
            if (event == null) {
                return Result.failure(ErrorDetail.of("EVENT_NOT_FOUND",
                    "Event not found: " + update.eventId(), "event.not.found"));
            }

            event.setStatus(update.status());
            if (update.processedAt() != null) {
                event.setProcessedAt(update.processedAt());
            }
            if (update.error() != null) {
                event.setLastError(update.error());
                event.setRetryCount(event.getRetryCount() + 1);
            }

            entityManager.merge(event);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("EVENT_UPDATE_FAILED",
                "Failed to update event status: " + e.getMessage(), "event.update.failed"));
        }
    }

    /**
     * Record for event status updates
     */
    private record EventStatusUpdate(
        String eventId,
        OutboxEventEntity.OutboxEventStatus status,
        String error,
        Instant processedAt
    ) {}
}