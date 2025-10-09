package com.bcbs239.regtech.iam.infrastructure.outbox;

import com.bcbs239.regtech.core.outbox.OutboxMessage;
import com.bcbs239.regtech.core.outbox.OutboxMessageRepository;
import com.bcbs239.regtech.core.outbox.OutboxOptions;
import com.bcbs239.regtech.core.outbox.ProcessOutboxJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bcbs239.regtech.iam.infrastructure.database.entities.OutboxEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.bcbs239.regtech.core.outbox.ProcessOutboxEventPublisher;
import com.bcbs239.regtech.core.events.GenericOutboxEventProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * IAM bounded-context wiring for the generic outbox processor.
 * Loads messages from the common outbox table scoped by schema (if used).
 */
@Configuration
public class IamOutboxWiring {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public OutboxOptions iamOutboxOptions() {
        return new OutboxOptions(50, Duration.ofSeconds(10), "iam", true); // Enable parallel processing
    }

    @Bean
    public OutboxMessageRepository iamOutboxRepository() {
        return new OutboxMessageRepository() {
            @Override
            public Function<Integer, List<OutboxMessage>> messageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC", OutboxEventEntity.class)
                    .setParameter("status", OutboxEventEntity.OutboxEventStatus.PENDING)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new OutboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getCreatedAt(), e.getAggregateId()))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    try {
                        int updated = em.createQuery("UPDATE OutboxEventEntity o SET o.status = :status, o.processedAt = :processedAt WHERE o.id = :id")
                            .setParameter("status", OutboxEventEntity.OutboxEventStatus.PROCESSED)
                            .setParameter("processedAt", java.time.Instant.now())
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public Function<Integer, List<OutboxMessage>> claimBatch() {
                return batchSize -> {
                    // Atomically claim by updating status from PENDING to PROCESSING and returning claimed rows.
                    // Use a two-step approach: select ids, then update using id list with optimistic locking via version.
                    var pending = em.createQuery(
                            "SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC", OutboxEventEntity.class)
                        .setParameter("status", OutboxEventEntity.OutboxEventStatus.PENDING)
                        .setMaxResults(batchSize)
                        .getResultList();

                    var ids = pending.stream().map(OutboxEventEntity::getId).toList();
                    if (ids.isEmpty()) return List.of();

                    int updated = em.createQuery("UPDATE OutboxEventEntity o SET o.status = :processing WHERE o.id IN :ids")
                        .setParameter("processing", OutboxEventEntity.OutboxEventStatus.PROCESSING)
                        .setParameter("ids", ids)
                        .executeUpdate();

                    if (updated <= 0) return List.of();

                    // Reload the claimed entities
                    return em.createQuery("SELECT o FROM OutboxEventEntity o WHERE o.id IN :ids", OutboxEventEntity.class)
                        .setParameter("ids", ids)
                        .getResultList()
                        .stream()
                        .map(e -> new OutboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getCreatedAt(), e.getAggregateId()))
                        .toList();
                };
            }

            @Override
            public java.util.function.BiFunction<String, String, Boolean> markFailed() {
                return (id, err) -> {
                    try {
                        int updated = em.createQuery("UPDATE OutboxEventEntity o SET o.status = :failed, o.lastError = :err, o.retryCount = o.retryCount + 1 WHERE o.id = :id")
                            .setParameter("failed", OutboxEventEntity.OutboxEventStatus.FAILED)
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
                        int updated = em.createQuery("UPDATE OutboxEventEntity o SET o.status = :pending WHERE o.id = :id AND o.retryCount < 3")
                            .setParameter("pending", OutboxEventEntity.OutboxEventStatus.PENDING)
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public Function<Integer, List<OutboxMessage>> failedMessageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT o FROM OutboxEventEntity o WHERE o.status = :status ORDER BY o.createdAt ASC", OutboxEventEntity.class)
                    .setParameter("status", OutboxEventEntity.OutboxEventStatus.FAILED)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new OutboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getCreatedAt(), e.getAggregateId()))
                    .toList();
            }
        };
    }

    @Bean
    @Qualifier("iamOutboxHandlers")
    public Map<String, Function<Object, Boolean>> iamOutboxHandlers(ObjectMapper objectMapper,
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
    public ProcessOutboxJob iamProcessOutboxJob(OutboxMessageRepository iamOutboxRepository,
                                                ObjectMapper objectMapper,
                                                Map<String, Function<Object, Boolean>> iamOutboxHandlers,
                                                OutboxOptions iamOutboxOptions) {
        return new ProcessOutboxJob(iamOutboxRepository, objectMapper, iamOutboxHandlers, iamOutboxOptions);
    }

    @Bean
    public ProcessOutboxEventPublisher iamOutboxEventPublisher(ProcessOutboxJob iamProcessOutboxJob) {
        return new ProcessOutboxEventPublisher(iamProcessOutboxJob);
    }

    @Bean
    public IamOutboxProcessor iamGenericOutboxProcessor(ProcessOutboxEventPublisher iamOutboxEventPublisher) {
        return new IamOutboxProcessor(iamOutboxEventPublisher);
    }
}
