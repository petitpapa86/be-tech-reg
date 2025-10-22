package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Core inbox configuration providing shared inbox infrastructure.
 * Handler registration is now handled automatically by IntegrationEventDispatcher.
 */
@Configuration
public class CoreInboxConfiguration {

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn(InboxMessageOperations jpaRepository) {
        return jpaRepository.findPendingMessagesFn();
    }
}