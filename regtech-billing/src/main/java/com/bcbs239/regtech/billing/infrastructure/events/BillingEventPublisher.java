package com.bcbs239.regtech.billing.infrastructure.events;

import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.infrastructure.entities.BillingDomainEventEntity;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Service for publishing billing domain events using the outbox pattern with closure-based dependencies.
 * Ensures reliable event delivery by persisting events in the same transaction as business data.
 */
@Service
@Transactional
public class BillingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BillingEventPublisher.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final EventSerializer eventSerializer;
    private final CrossModuleEventBus crossModuleEventBus;

    public BillingEventPublisher(EventSerializer eventSerializer,
                               CrossModuleEventBus crossModuleEventBus) {
        this.eventSerializer = eventSerializer;
        this.crossModuleEventBus = crossModuleEventBus;
    }

    /**
     * Publish a domain event using the outbox pattern with closure-based dependencies.
     */
    public Result<Void> publishEvent(Object event) {
        return publishEventWithClosures(
            event,
            eventSerializer::serialize,
            eventSaver(),
            crossModuleEventBus::publishEvent,
            () -> UUID.randomUUID().toString(),
            this::determineTargetModule
        );
    }

    /**
     * Pure function for event publishing with injected dependencies as closures.
     */
    static Result<Void> publishEventWithClosures(
            Object event,
            Function<Object, Result<String>> eventSerializer,
            Function<BillingDomainEventEntity, Result<Void>> eventSaver,
            Consumer<Object> eventPublisher,
            Supplier<String> eventIdGenerator,
            Function<Object, String> targetModuleDeterminer) {

        if (!(event instanceof BaseEvent)) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EVENT_TYPE",
                "Event must extend BaseEvent: " + event.getClass().getName(),
                "event.invalid.type"
            ));
        }

        BaseEvent baseEvent = (BaseEvent) event;
        String eventId = eventIdGenerator.get();
        String eventType = event.getClass().getName();
        String targetModule = targetModuleDeterminer.apply(event);

        // Serialize the event
        Result<String> serializationResult = eventSerializer.apply(event);
        if (serializationResult.isFailure()) {
            return Result.failure(serializationResult.getError().get());
        }

        // Store in outbox
        BillingDomainEventEntity eventEntity = new BillingDomainEventEntity(
            eventId,
            eventType,
            baseEvent.getCorrelationId(),
            baseEvent.getSourceModule(),
            targetModule,
            serializationResult.getValue().get()
        );

        Result<Void> saveResult = eventSaver.apply(eventEntity);
        if (saveResult.isFailure()) {
            return saveResult;
        }

        // Immediately try to publish the event
        try {
            eventPublisher.accept(event);
            eventEntity.markAsProcessed();
            eventSaver.apply(eventEntity); // Update the entity status
        } catch (Exception e) {
            eventEntity.markAsFailed("Immediate publication failed: " + e.getMessage());
            eventSaver.apply(eventEntity); // Update the entity status
            // Don't fail the overall operation - the event will be retried later
        }

        return Result.success(null);
    }

    /**
     * Process pending events from the outbox using closure-based approach.
     */
    @Transactional
    public void processPendingEvents() {
        processPendingEventsWithClosures(
            pendingEventsFinder(),
            eventDeserializer(),
            crossModuleEventBus::publishEvent,
            eventUpdater()
        );
    }

    /**
     * Pure function for processing pending events with closures.
     */
    static void processPendingEventsWithClosures(
            Supplier<List<BillingDomainEventEntity>> pendingEventsFinder,
            Function<BillingDomainEventEntity, Result<Object>> eventDeserializer,
            Consumer<Object> eventPublisher,
            Function<BillingDomainEventEntity, Result<Void>> eventUpdater) {

        List<BillingDomainEventEntity> pendingEvents = pendingEventsFinder.get();
        
        for (BillingDomainEventEntity eventEntity : pendingEvents) {
            try {
                eventEntity.markAsProcessing();
                eventUpdater.apply(eventEntity);

                Result<Object> deserializationResult = eventDeserializer.apply(eventEntity);
                if (deserializationResult.isFailure()) {
                    eventEntity.markAsFailed("Deserialization failed: " + deserializationResult.getError().get().getMessage());
                    eventUpdater.apply(eventEntity);
                    continue;
                }

                Object event = deserializationResult.getValue().get();
                eventPublisher.accept(event);

                eventEntity.markAsProcessed();
                eventUpdater.apply(eventEntity);

            } catch (Exception e) {
                eventEntity.markAsFailed("Publication failed: " + e.getMessage());
                
                if (!eventEntity.canRetry(3)) {
                    eventEntity.markAsDeadLetter();
                }
                
                eventUpdater.apply(eventEntity);
            }
        }
    }

    /**
     * Retry failed events that can be retried.
     */
    @Transactional
    public void retryFailedEvents(int maxRetries) {
        retryFailedEventsWithClosures(
            () -> retryableEventsFinder(maxRetries),
            eventDeserializer(),
            crossModuleEventBus::publishEvent,
            eventUpdater()
        );
    }

    /**
     * Pure function for retrying failed events with closures.
     */
    static void retryFailedEventsWithClosures(
            Supplier<List<BillingDomainEventEntity>> retryableEventsFinder,
            Function<BillingDomainEventEntity, Result<Object>> eventDeserializer,
            Consumer<Object> eventPublisher,
            Function<BillingDomainEventEntity, Result<Void>> eventUpdater) {

        // Reuse the same logic as processing pending events
        processPendingEventsWithClosures(retryableEventsFinder, eventDeserializer, eventPublisher, eventUpdater);
    }

    // Closure implementations using EntityManager

    private Function<BillingDomainEventEntity, Result<Void>> eventSaver() {
        return eventEntity -> {
            try {
                if (eventEntity.getId() == null) {
                    entityManager.persist(eventEntity);
                } else {
                    entityManager.merge(eventEntity);
                }
                return Result.success(null);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of(
                    "EVENT_STORAGE_FAILED",
                    "Failed to store event in outbox: " + e.getMessage(),
                    "event.storage.failed"
                ));
            }
        };
    }

    private Supplier<List<BillingDomainEventEntity>> pendingEventsFinder() {
        return () -> {
            TypedQuery<BillingDomainEventEntity> query = entityManager.createQuery(
                "SELECT e FROM BillingDomainEventEntity e WHERE e.processingStatus = 'PENDING' ORDER BY e.createdAt ASC",
                BillingDomainEventEntity.class
            );
            return query.getResultList();
        };
    }

    private List<BillingDomainEventEntity> retryableEventsFinder(int maxRetries) {
        TypedQuery<BillingDomainEventEntity> query = entityManager.createQuery(
            "SELECT e FROM BillingDomainEventEntity e WHERE e.processingStatus = 'FAILED' AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC",
            BillingDomainEventEntity.class
        );
        query.setParameter("maxRetries", maxRetries);
        return query.getResultList();
    }

    private Function<BillingDomainEventEntity, Result<Object>> eventDeserializer() {
        return eventEntity -> {
            try {
                Class<?> eventClass = Class.forName(eventEntity.getEventType());
                return eventSerializer.deserialize(eventEntity.getEventData(), eventClass);
            } catch (ClassNotFoundException e) {
                return Result.failure(ErrorDetail.of(
                    "EVENT_TYPE_NOT_FOUND",
                    "Event type not found: " + eventEntity.getEventType(),
                    "event.type.not.found"
                ));
            }
        };
    }

    private Function<BillingDomainEventEntity, Result<Void>> eventUpdater() {
        return eventEntity -> {
            try {
                entityManager.merge(eventEntity);
                return Result.success(null);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of(
                    "EVENT_UPDATE_FAILED",
                    "Failed to update event: " + e.getMessage(),
                    "event.update.failed"
                ));
            }
        };
    }

    /**
     * Determine the target module for an event based on its type.
     */
    private String determineTargetModule(Object event) {
        if (event instanceof PaymentVerifiedEvent) {
            return "iam"; // IAM context needs to know about payment verification
        } else if (event instanceof BillingAccountStatusChangedEvent) {
            return "iam"; // IAM context may need to know about account status changes
        } else if (event instanceof InvoiceGeneratedEvent) {
            return "notification"; // Notification system for invoice notifications
        } else if (event instanceof SubscriptionCancelledEvent) {
            return "iam"; // IAM context for cleanup processes
        }
        
        // Default to null for internal billing events
        return null;
    }

    /**
     * Get event publishing statistics for monitoring.
     */
    public EventPublishingStats getStats() {
        return getStatsWithClosures(eventCountByStatus());
    }

    /**
     * Pure function for getting statistics with closures.
     */
    static EventPublishingStats getStatsWithClosures(Function<BillingDomainEventEntity.ProcessingStatus, Long> eventCounter) {
        long pending = eventCounter.apply(BillingDomainEventEntity.ProcessingStatus.PENDING);
        long processing = eventCounter.apply(BillingDomainEventEntity.ProcessingStatus.PROCESSING);
        long processed = eventCounter.apply(BillingDomainEventEntity.ProcessingStatus.PROCESSED);
        long failed = eventCounter.apply(BillingDomainEventEntity.ProcessingStatus.FAILED);
        long deadLetter = eventCounter.apply(BillingDomainEventEntity.ProcessingStatus.DEAD_LETTER);

        return new EventPublishingStats(pending, processing, processed, failed, deadLetter);
    }

    private Function<BillingDomainEventEntity.ProcessingStatus, Long> eventCountByStatus() {
        return status -> {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(e) FROM BillingDomainEventEntity e WHERE e.processingStatus = :status",
                Long.class
            );
            query.setParameter("status", status);
            return query.getSingleResult();
        };
    }

    /**
     * Statistics record for event publishing monitoring.
     */
    public record EventPublishingStats(
        long pending,
        long processing,
        long processed,
        long failed,
        long deadLetter
    ) {}
}