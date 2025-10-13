package com.bcbs239.regtech.billing.infrastructure.outbox;

import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for the outbox pattern in the billing context.
 * Provides reliable event publishing infrastructure.
 */
@Configuration
@EnableScheduling
public class BillingOutboxWiring {

    /**
     * Outbox publisher bean for publishing events reliably.
     */
    @Bean
    public OutboxPublisher billingOutboxPublisher(
            BillingOutboxMessageRepository outboxMessageRepository,
            ObjectMapper objectMapper) {
        return new OutboxPublisherImpl(outboxMessageRepository, objectMapper);
    }

    /**
     * Outbox processor job for publishing stored events.
     */
    @Bean
    public ProcessOutboxJob billingOutboxProcessor(
            BillingOutboxMessageRepository outboxMessageRepository,
            CrossModuleEventBus eventBus,
            ObjectMapper objectMapper) {
        return new ProcessOutboxJob(outboxMessageRepository, eventBus, objectMapper);
    }
}