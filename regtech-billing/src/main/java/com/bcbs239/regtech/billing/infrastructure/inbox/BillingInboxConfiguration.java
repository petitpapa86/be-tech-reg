package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.core.inbox.IdempotentIntegrationEventHandler;
import com.bcbs239.regtech.core.inbox.InboxMessageConsumerRepository;
import com.bcbs239.regtech.core.inbox.InboxMessageRepository;
import com.bcbs239.regtech.core.inbox.InboxOptions;
import com.bcbs239.regtech.core.inbox.ProcessInboxJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Configuration for inbox processing in the billing bounded context.
 * Wires up integration event handlers and provides scheduled processing.
 */
@Configuration
public class BillingInboxConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BillingInboxConfiguration.class);

    @PersistenceContext
    private EntityManager em;

    /**
     * Configure inbox processing options for billing context.
     */
    @Bean
    public InboxOptions billingInboxOptions() {
        return new InboxOptions(50, Duration.ofSeconds(30), "billing", true);
    }

    /**
     * Map of integration event handlers by event type for inbox processing.
     * Currently handles UserRegisteredIntegrationEvent from IAM context.
     * Handlers are wrapped with IdempotentIntegrationEventHandler for duplicate prevention.
     */
    @Bean
    @Qualifier("billingInboxHandlers")
    public Map<String, Function<Object, Boolean>> billingInboxHandlers(
            List<IIntegrationEventHandler<?>> handlers,
            InboxMessageConsumerRepository consumerRepository) {

        Map<String, Function<Object, Boolean>> handlerMap = new HashMap<>();

        // Register handlers by their event class name
        for (IIntegrationEventHandler<?> handler : handlers) {
            // Wrap handler with idempotency decorator
            IdempotentIntegrationEventHandler<?> idempotentHandler =
                new IdempotentIntegrationEventHandler<>(handler, consumerRepository);

            String eventType = handler.getEventClass().getSimpleName();
            handlerMap.put(eventType, event -> {
                try {
                    // Use raw type to bypass generic type checking and cast event to IntegrationEvent
                    @SuppressWarnings({"rawtypes", "unchecked", "type safety"})
                    IdempotentIntegrationEventHandler rawHandler = idempotentHandler;
                    rawHandler.handle((com.bcbs239.regtech.core.application.IntegrationEvent) event);
                    return true;
                } catch (ClassCastException e) {
                    logger.error("Event type mismatch for handler {}: expected {}, got {}",
                        handler.getHandlerName(), handler.getEventClass().getSimpleName(), event.getClass().getSimpleName(), e);
                    return false;
                } catch (Exception e) {
                    logger.error("Error processing event {} with handler {}: {}",
                        eventType, handler.getHandlerName(), e.getMessage(), e);
                    return false;
                }
            });
            logger.info("Registered idempotent inbox handler: {} for event type: {}", handler.getHandlerName(), eventType);
        }

        return handlerMap;
    }

    /**
     * Inbox repository for billing context using shared inbox infrastructure.
     */
    @Bean
    public InboxMessageRepository billingInboxRepository() {
        return new InboxMessageRepository() {
            @Override
            public Function<Integer, List<com.bcbs239.regtech.core.inbox.InboxMessage>> messageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT i FROM InboxMessageEntity i WHERE i.processedAt IS NULL ORDER BY i.receivedAt ASC", com.bcbs239.regtech.core.inbox.InboxMessageEntity.class)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new com.bcbs239.regtech.core.inbox.InboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getReceivedAt(), e.getAggregateId()))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    try {
                        int updated = em.createQuery("UPDATE InboxMessageEntity i SET i.processedAt = :processedAt WHERE i.id = :id")
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
            public Function<Integer, List<com.bcbs239.regtech.core.inbox.InboxMessage>> failedMessageLoader() {
                return batchSize -> em.createQuery(
                        "SELECT i FROM InboxMessageEntity i WHERE i.processedAt IS NULL AND i.retryCount > 3 ORDER BY i.receivedAt ASC", com.bcbs239.regtech.core.inbox.InboxMessageEntity.class)
                    .setMaxResults(batchSize)
                    .getResultList()
                    .stream()
                    .map(e -> new com.bcbs239.regtech.core.inbox.InboxMessage(e.getId(), e.getEventType(), e.getEventData(), e.getReceivedAt(), e.getAggregateId()))
                    .toList();
            }

            @Override
            public BiFunction<String, String, Boolean> markFailed() {
                return (id, errorMessage) -> {
                    try {
                        int updated = em.createQuery("UPDATE InboxMessageEntity i SET i.retryCount = i.retryCount + 1, i.errorMessage = :error WHERE i.id = :id")
                            .setParameter("error", errorMessage)
                            .setParameter("id", id)
                            .executeUpdate();
                        return updated > 0;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }
        };
    }

    /**
     * Process inbox job for billing context.
     */
    @Bean
    @Primary
    public ProcessInboxJob billingProcessInboxJob(
            @Qualifier("billingInboxRepository") InboxMessageRepository inboxRepository,
            ObjectMapper objectMapper,
            @Qualifier("billingInboxHandlers") Map<String, Function<Object, Boolean>> billingInboxHandlers,
            @Qualifier("billingInboxOptions") InboxOptions inboxOptions) {
        return new ProcessInboxJob(inboxRepository, objectMapper, billingInboxHandlers, inboxOptions);
    }

    /**
     * Scheduled job component that processes inbox messages periodically.
     * Runs every 30 seconds to process pending integration events.
     */
    @Component
    public static class ProcessInboxScheduler {

        private static final Logger logger = LoggerFactory.getLogger(ProcessInboxScheduler.class);

        private final ProcessInboxJob processInboxJob;

        public ProcessInboxScheduler(@Qualifier("billingProcessInboxJob") ProcessInboxJob processInboxJob) {
            this.processInboxJob = processInboxJob;
            logger.info("🚀 ProcessInboxScheduler initialized for billing context");
        }

        /**
         * Process inbox messages every 30 seconds.
         * This ensures timely processing of integration events from other bounded contexts.
         */
        @Scheduled(fixedDelay = 30000) // 30 seconds
        public void processInboxMessages() {
            try {
                logger.debug("🔄 Processing billing inbox messages...");
                int processedCount = processInboxJob.runOnce();

                if (processedCount > 0) {
                    logger.info("📨 Processed {} inbox messages in billing context", processedCount);
                } else {
                    logger.debug("📭 No inbox messages to process");
                }

            } catch (Exception e) {
                logger.error("Failed to process inbox messages: {}", e.getMessage(), e);
            }
        }
    }
}