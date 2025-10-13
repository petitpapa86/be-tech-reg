package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.inbox.InboxMessage;
import com.bcbs239.regtech.core.inbox.InboxMessageRepository;
import com.bcbs239.regtech.core.inbox.InboxOptions;
import com.bcbs239.regtech.core.inbox.ProcessInboxJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.bcbs239.regtech.core.inbox.ProcessInboxEventPublisher;
import com.bcbs239.regtech.core.events.GenericInboxEventProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Billing bounded-context wiring for the generic inbox processor.
 * Loads messages from the common inbox table scoped by schema (if used).
 * Processes integration events from other bounded contexts.
 */
@Configuration
public class BillingInboxWiring {

    @PersistenceContext
    private EntityManager em;

    /**
     * Closure-based API: saver for InboxEventEntity
     */
    @Bean
    public java.util.function.Consumer<com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity> billingInboxSaver() {
        return entity -> {
            try {
                System.out.println("billingInboxSaver: persisting inbox event id=" + entity.getId() + " type=" + entity.getEventType());
                em.persist(entity);
                // ensure immediate write for tests
                em.flush();
                System.out.println("billingInboxSaver: persisted inbox event id=" + entity.getId());
            } catch (Exception ex) {
                System.err.println("billingInboxSaver: failed to persist inbox event: " + ex.getMessage());
                throw ex;
            }
        };
    }

    /**
     * Closure-based loader for pending inbox events
     */
    @Bean
    public java.util.function.Function<Integer, java.util.List<com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity>> billingInboxLoader() {
        return batchSize -> em.createQuery(
                "SELECT i FROM billingInboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity.class)
            .setParameter("status", com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity.ProcessingStatus.PENDING)
            .setMaxResults(batchSize)
            .getResultList();
    }

    @Bean
    public java.util.function.Function<String, Boolean> billingMarkProcessed() {
        return id -> {
            try {
                int updated = em.createQuery("UPDATE billingInboxEventEntity i SET i.processingStatus = :processed WHERE i.id = :id")
                    .setParameter("processed", com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity.ProcessingStatus.PROCESSED)
                    .setParameter("id", id)
                    .executeUpdate();
                return updated > 0;
            } catch (Exception ex) {
                return false;
            }
        };
    }

    @Bean
    public java.util.function.BiFunction<String, String, Boolean> billingMarkFailed() {
        return (id, err) -> {
            try {
                int updated = em.createQuery("UPDATE billingInboxEventEntity i SET i.processingStatus = :failed, i.lastError = :err, i.retryCount = i.retryCount + 1 WHERE i.id = :id")
                    .setParameter("failed", com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity.ProcessingStatus.FAILED)
                    .setParameter("err", err)
                    .setParameter("id", id)
                    .executeUpdate();
                return updated > 0;
            } catch (Exception ex) {
                return false;
            }
        };
    }

    @Bean
    public java.util.function.Function<String, java.util.List<com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity>> billingFindByEventType() {
        return type -> em.createQuery("SELECT i FROM billingInboxEventEntity i WHERE i.eventType = :type ORDER BY i.receivedAt DESC", com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity.class)
            .setParameter("type", type)
            .getResultList();
    }

    @Bean
    public InboxOptions billingInboxOptions() {
        return new InboxOptions(50, Duration.ofSeconds(10), "billing", true); // Enable parallel processing
    }

    @Bean
    public InboxMessageRepository billingInboxRepository() {
        return new InboxMessageRepository() {
            @Override
            public Function<Integer, List<InboxMessage>> messageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT i FROM billingInboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxEventEntity.class)
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
                        int updated = em.createQuery("UPDATE billingInboxEventEntity i SET i.processingStatus = :processed WHERE i.id = :id")
                            .setParameter("processed", InboxEventEntity.ProcessingStatus.PROCESSED)
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }

            @Override
            public java.util.function.BiFunction<String, String, Boolean> markFailed() {
                return (id, err) -> {
                    try {
                        int updated = em.createQuery("UPDATE billingInboxEventEntity i SET i.processingStatus = :failed, i.lastError = :err, i.retryCount = i.retryCount + 1 WHERE i.id = :id")
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
                        int updated = em.createQuery("UPDATE billingInboxEventEntity i SET i.processingStatus = :pending WHERE i.id = :id AND i.retryCount < 3")
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
                        "SELECT i FROM billingInboxEventEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxEventEntity.class)
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
    @Qualifier("billingInboxHandlers")
    public Map<String, Function<Object, Boolean>> billingInboxHandlers(ObjectMapper objectMapper,
                                                                       java.util.List<IdempotentIntegrationEventHandler<?>> handlers) {
        Map<String, Function<Object, Boolean>> map = new HashMap<>();

        // Log the discovered handlers
        System.out.println("BillingInboxWiring: Discovered " + handlers.size() + " IdempotentIntegrationEventHandler beans");
        for (var h : handlers) {
            System.out.println("BillingInboxWiring: Handler for eventType: " + h.eventType() + ", class: " + h.getClass().getSimpleName());
        }

        // Register discovered typed handlers
        for (var h : handlers) {
            map.put(h.eventType(), raw -> {
                try {
                    // Convert the raw Map to the typed event object
                    Object typed = objectMapper.convertValue(raw, h.eventClass());
                    // Use raw type cast to avoid generic bounds issues
                    @SuppressWarnings("rawtypes")
                    IdempotentIntegrationEventHandler dh = (IdempotentIntegrationEventHandler) h;
                    boolean result = dh.handle((com.bcbs239.regtech.core.events.BaseEvent) typed);
                    return result;
                } catch (Exception e) {
                    return false;
                }
            });
        }

        System.out.println("BillingInboxWiring: Registered " + map.size() + " handlers in billingInboxHandlers map");
        return map;
    }

    @Bean
    public ProcessInboxJob billingProcessInboxJob(InboxMessageRepository billingInboxRepository,
                                                  ObjectMapper objectMapper,
                                                  @Qualifier("billingInboxHandlers") Map<String, Function<Object, Boolean>> billingInboxHandlers,
                                                  InboxOptions billingInboxOptions) {
        return new ProcessInboxJob(billingInboxRepository, objectMapper, billingInboxHandlers, billingInboxOptions);
    }

    @Bean
    public ProcessInboxEventPublisher billingInboxEventPublisher(ProcessInboxJob billingProcessInboxJob) {
        return new ProcessInboxEventPublisher(billingProcessInboxJob);
    }

    @Bean
    public BillingInboxProcessor billingGenericInboxProcessor(ProcessInboxEventPublisher billingInboxEventPublisher) {
        return new BillingInboxProcessor(billingInboxEventPublisher);
    }

    @Bean
    public java.util.function.BiFunction<String, String, Boolean> billingDuplicateChecker() {
        return (eventType, aggregateId) -> {
            try {
                Long count = em.createQuery("SELECT COUNT(i) FROM billingInboxEventEntity i WHERE i.eventType = :eventType AND i.aggregateId = :aggregateId", Long.class)
                    .setParameter("eventType", eventType)
                    .setParameter("aggregateId", aggregateId)
                    .getSingleResult();
                return count > 0;
            } catch (Exception ex) {
                System.err.println("billingDuplicateChecker: failed to check for duplicates: " + ex.getMessage());
                return false; // Assume no duplicate on error to avoid blocking processing
            }
        };
    }
}