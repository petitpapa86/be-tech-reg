package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Core inbox configuration providing shared inbox infrastructure.
 * This configuration provides the InboxMessageRepository bean.
 * Handler registration is now handled automatically by IntegrationEventDispatcher.
 */
@Configuration
public class CoreInboxConfiguration {

    @Bean
    public InboxMessageRepository inboxMessageRepository(InboxMessageOperations jpaRepository) {
        return new InboxMessageRepository() {
            @Override
            public Function<Integer, List<InboxMessageDto>> messageLoader() {
                return batchSize -> jpaRepository.findPendingMessagesFn().apply(InboxMessageEntity.ProcessingStatus.PENDING)
                    .stream()
                    .limit(batchSize)
                    .map(entity -> new InboxMessageDto(
                        entity.getId(),
                        entity.getEventType(),
                        entity.getEventData(),
                        entity.getProcessingStatus().name(),
                        entity.getReceivedAt().toString(),
                        entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null
                    ))
                    .toList();
            }

            @Override
            public Function<String, Boolean> markProcessed() {
                return id -> {
                    int updated = jpaRepository.markAsProcessedFn().apply(new InboxMessageOperations.MarkAsProcessedRequest(id, java.time.Instant.now()));
                    return updated > 0;
                };
            }

            @Override
            public Function<Integer, List<InboxMessageDto>> failedMessageLoader() {
                return batchSize -> jpaRepository.findFailedMessagesEligibleForRetryFn().apply(java.time.Instant.now())
                    .stream()
                    .limit(batchSize)
                    .map(entity -> new InboxMessageDto(
                        entity.getId(),
                        entity.getEventType(),
                        entity.getEventData(),
                        entity.getProcessingStatus().name(),
                        entity.getReceivedAt().toString(),
                        entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null
                    ))
                    .toList();
            }

            @Override
            public BiFunction<String, String, Boolean> markFailed() {
                return (id, errorMessage) -> {
                    // For simplicity, mark as permanently failed for now
                    // In a real implementation, you might want to implement retry logic
                    int updated = jpaRepository.markAsPermanentlyFailedFn().apply(new InboxMessageOperations.MarkAsPermanentlyFailedRequest(id, errorMessage));
                    return updated > 0;
                };
            }
        };
    }

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn(InboxMessageOperations jpaRepository) {
        return jpaRepository.findPendingMessagesFn();
    }
}