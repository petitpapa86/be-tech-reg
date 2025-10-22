package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;

/**
 * Configuration for inbox processing beans.
 */
@Configuration
public class InboxProcessingConfiguration {

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn(InboxMessageOperations jpaRepository) {
        return jpaRepository.findPendingMessagesFn();
    }
}