package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Core inbox configuration providing shared inbox infrastructure.
 * This configuration provides the InboxMessageRepository bean used across bounded contexts.
 */
@Configuration
public class CoreInboxConfiguration {

    @Bean
    public InboxMessageRepository inboxMessageRepository(InboxMessageJpaRepository jpaRepository) {
        return new InboxMessageRepository() {
            @Override
            public Function<Integer, List<InboxMessage>> messageLoader() {
                return batchSize -> jpaRepository.findPendingMessages(InboxMessageEntity.ProcessingStatus.PENDING)
                    .stream()
                    .limit(batchSize)
                    .map(entity -> new InboxMessage(
                        entity.getId(),
                        entity.getEventType(),
                        entity.getEventData(),
                        entity.getReceivedAt(),
                        entity.getAggregateId()
                    ))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    int updated = jpaRepository.markAsProcessed(id, java.time.Instant.now());
                    return updated > 0;
                };
            }

            @Override
            public Function<Integer, List<InboxMessage>> failedMessageLoader() {
                return batchSize -> jpaRepository.findFailedMessagesEligibleForRetry(java.time.Instant.now())
                    .stream()
                    .limit(batchSize)
                    .map(entity -> new InboxMessage(
                        entity.getId(),
                        entity.getEventType(),
                        entity.getEventData(),
                        entity.getReceivedAt(),
                        entity.getAggregateId()
                    ))
                    .toList();
            }

            @Override
            public BiFunction<String, String, Boolean> markFailed() {
                return (id, errorMessage) -> {
                    // For simplicity, mark as permanently failed for now
                    // In a real implementation, you might want to implement retry logic
                    int updated = jpaRepository.markAsPermanentlyFailed(id, errorMessage);
                    return updated > 0;
                };
            }
        };
    }
}