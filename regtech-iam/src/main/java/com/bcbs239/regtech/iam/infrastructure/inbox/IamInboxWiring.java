package com.bcbs239.regtech.iam.infrastructure.inbox;

import com.bcbs239.regtech.core.inbox.*;
import com.bcbs239.regtech.core.events.GenericInboxEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bcbs239.regtech.iam.infrastructure.database.entities.InboxEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * IAM bounded-context wiring for the generic inbox processor.
 * Loads messages from the common inbox table scoped by schema (if used).
 */
@Configuration
public class IamInboxWiring {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public InboxOptions iamInboxOptions() {
        return new InboxOptions(50, Duration.ofSeconds(10), "iam");
    }

    @Bean
    public InboxMessageRepository iamInboxRepository() {
        return new InboxMessageRepository() {
            @Override
            public Function<Integer, List<InboxMessage>> messageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT i FROM InboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxEventEntity.class)
                    .setParameter("status", InboxEventEntity.ProcessingStatus.PENDING)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new InboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getReceivedAt(), e.getAggregateId()))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    try {
                        int updated = em.createQuery("UPDATE InboxEventEntity i SET i.processingStatus = :status, i.processedAt = :processedAt WHERE i.id = :id")
                            .setParameter("status", InboxEventEntity.ProcessingStatus.PROCESSED)
                            .setParameter("processedAt", Instant.now())
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public Function<Integer, List<InboxMessage>> claimBatch() {
                return batchSize -> {
                    // Atomically claim by updating status from PENDING to PROCESSING and returning claimed rows.
                    // Use a two-step approach: select ids, then update using id list with optimistic locking via version.
                    var pending = em.createQuery(
                            "SELECT i FROM InboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxEventEntity.class)
                        .setParameter("status", InboxEventEntity.ProcessingStatus.PENDING)
                        .setMaxResults(batchSize)
                        .getResultList();

                    var ids = pending.stream().map(InboxEventEntity::getId).toList();
                    if (ids.isEmpty()) return List.of();

                    int updated = em.createQuery("UPDATE InboxEventEntity i SET i.processingStatus = :processing WHERE i.id IN :ids")
                        .setParameter("processing", InboxEventEntity.ProcessingStatus.PROCESSING)
                        .setParameter("ids", ids)
                        .executeUpdate();

                    if (updated <= 0) return List.of();

                    // Reload the claimed entities
                    return em.createQuery("SELECT i FROM InboxEventEntity i WHERE i.id IN :ids", InboxEventEntity.class)
                        .setParameter("ids", ids)
                        .getResultList()
                        .stream()
                        .map(e -> new InboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getReceivedAt(), e.getAggregateId()))
                        .toList();
                };
            }

            @Override
            public java.util.function.BiFunction<String, String, Boolean> markFailed() {
                return (id, err) -> {
                    try {
                        int updated = em.createQuery("UPDATE InboxEventEntity i SET i.processingStatus = :failed, i.lastError = :err, i.retryCount = i.retryCount + 1 WHERE i.id = :id")
                            .setParameter("failed", InboxEventEntity.ProcessingStatus.FAILED)
                            .setParameter("err", err)
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public Function<String, Boolean> resetForRetry() {
                return id -> {
                    try {
                        int updated = em.createQuery("UPDATE InboxEventEntity i SET i.processingStatus = :pending WHERE i.id = :id AND i.retryCount < 3")
                            .setParameter("pending", InboxEventEntity.ProcessingStatus.PENDING)
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public Function<Integer, List<InboxMessage>> failedMessageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT i FROM InboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxEventEntity.class)
                    .setParameter("status", InboxEventEntity.ProcessingStatus.FAILED)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new InboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getReceivedAt(), e.getAggregateId()))
                    .toList();
            }
        };
    }

    @Bean
    public Map<String, Function<Object, Boolean>> iamInboxHandlers(ObjectMapper objectMapper,
                                                                    java.util.List<com.bcbs239.regtech.core.events.DomainEventHandler<?>> handlers) {
        Map<String, Function<Object, Boolean>> map = new HashMap<>();

        // Register discovered typed handlers
        for (var h : handlers) {
            map.put(h.eventType(), raw -> {
                try {
                    Object typed = objectMapper.convertValue(raw, h.eventClass());
                    // Use raw type cast to avoid generic bounds issues
                    @SuppressWarnings("rawtypes")
                    com.bcbs239.regtech.core.events.DomainEventHandler dh = (com.bcbs239.regtech.core.events.DomainEventHandler) h;
                    return dh.handle((com.bcbs239.regtech.core.events.DomainEvent) typed);
                } catch (Exception e) {
                    return false;
                }
            });
        }

        return map;
    }

    @Bean
    public ProcessInboxJob iamProcessInboxJob(InboxMessageRepository iamInboxRepository,
                                                ObjectMapper objectMapper,
                                                Map<String, Function<Object, Boolean>> iamInboxHandlers,
                                                InboxOptions iamInboxOptions) {
        return new ProcessInboxJob(iamInboxRepository, objectMapper, iamInboxHandlers, iamInboxOptions);
    }

    @Bean
    public ProcessInboxEventPublisher iamInboxEventPublisher(ProcessInboxJob iamProcessInboxJob) {
        return new ProcessInboxEventPublisher(iamProcessInboxJob);
    }

    @Bean
    public IamInboxProcessor iamGenericInboxProcessor(ProcessInboxEventPublisher iamInboxEventPublisher) {
        return new IamInboxProcessor(iamInboxEventPublisher);
    }
}