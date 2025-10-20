package com.bcbs239.regtech.billing.infrastructure.outbox;

import com.bcbs239.regtech.core.outbox.OutboxMessage;
import com.bcbs239.regtech.core.outbox.OutboxMessageRepository;
import com.bcbs239.regtech.core.outbox.OutboxOptions;
import com.bcbs239.regtech.core.outbox.ProcessOutboxJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;

/**
 * Billing bounded-context wiring for the generic outbox processor.
 * Loads messages from the billing outbox table.
 */
@Configuration
public class BillingOutboxWiring {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public OutboxOptions billingOutboxOptions() {
        return new OutboxOptions(50, Duration.ofSeconds(10), "billing", true); // Enable parallel processing
    }

    @Bean
    public OutboxMessageRepository billingOutboxRepository() {
        return new OutboxMessageRepository() {
            @Override
            public Function<Integer, List<OutboxMessage>> messageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT o FROM OutboxMessage o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC", com.bcbs239.regtech.billing.infrastructure.outbox.OutboxMessage.class)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(billingMessage -> new OutboxMessage(billingMessage.getId().toString(), billingMessage.getEventType(), billingMessage.getPayload(), billingMessage.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(), billingMessage.getCorrelationId()))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    try {
                        int updated = em.createQuery("UPDATE OutboxMessage o SET o.processedAt = :processedAt WHERE o.id = :id")
                            .setParameter("processedAt", java.time.LocalDateTime.now())
                            .setParameter("id", java.util.UUID.fromString(id))
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
                    // For billing, we'll use a simpler approach since the entity doesn't have status
                    var pending = em.createQuery(
                            "SELECT o FROM OutboxMessage o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC", com.bcbs239.regtech.billing.infrastructure.outbox.OutboxMessage.class)
                        .setMaxResults(batchSize)
                        .getResultList();

                    var ids = pending.stream().map(com.bcbs239.regtech.billing.infrastructure.outbox.OutboxMessage::getId).toList();
                    if (ids.isEmpty()) return List.of();

                    // Mark as processed immediately for simplicity
                    int updated = em.createQuery("UPDATE OutboxMessage o SET o.processedAt = :processedAt WHERE o.id IN :ids")
                        .setParameter("processedAt", java.time.LocalDateTime.now())
                        .setParameter("ids", ids)
                        .executeUpdate();

                    if (updated <= 0) return List.of();

                    // Return the messages
                    return pending.stream()
                        .map(billingMessage -> new OutboxMessage(billingMessage.getId().toString(), billingMessage.getEventType(), billingMessage.getPayload(), billingMessage.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(), billingMessage.getCorrelationId()))
                        .toList();
                };
            }

            @Override
            public BiFunction<String, String, Boolean> markFailed() {
                return (id, errorMessage) -> {
                    try {
                        // For billing, we'll increment retry count and set error message
                        int updated = em.createQuery("UPDATE OutboxMessage o SET o.retryCount = o.retryCount + 1, o.errorMessage = :error WHERE o.id = :id")
                            .setParameter("error", errorMessage)
                            .setParameter("id", java.util.UUID.fromString(id))
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
                        "SELECT o FROM OutboxMessage o WHERE o.processedAt IS NULL AND o.retryCount > 3 ORDER BY o.createdAt ASC", com.bcbs239.regtech.billing.infrastructure.outbox.OutboxMessage.class)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(billingMessage -> new OutboxMessage(billingMessage.getId().toString(), billingMessage.getEventType(), billingMessage.getPayload(), billingMessage.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(), billingMessage.getCorrelationId()))
                    .toList();
            }
        };
    }

    @Bean
    public ProcessOutboxJob billingOutboxProcessor(
            @Qualifier("billingOutboxRepository") OutboxMessageRepository repository,
            ObjectMapper objectMapper,
            @Qualifier("billingOutboxHandlers") Map<String, Function<Object, Boolean>> handlersByEventType,
            @Qualifier("billingOutboxOptions") OutboxOptions options) {
        return new ProcessOutboxJob(repository, objectMapper, handlersByEventType, options);
    }

    @Bean
    public Map<String, Function<Object, Boolean>> billingOutboxHandlers() {
        // TODO: Register billing-specific event handlers
        return new HashMap<>();
    }
}