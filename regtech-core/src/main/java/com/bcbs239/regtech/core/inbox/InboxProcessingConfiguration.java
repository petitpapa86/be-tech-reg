package com.bcbs239.regtech.core.inbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;

/**
 * Configuration for inbox processing beans.
 */
@Configuration
public class InboxProcessingConfiguration {

    @Autowired
    private InboxMessageOperations inboxMessageOperations;

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn() {
        return inboxMessageOperations.findPendingMessagesFn();
    }
}