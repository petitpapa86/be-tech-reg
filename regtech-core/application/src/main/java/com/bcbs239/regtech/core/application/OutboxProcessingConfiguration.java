package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.domain.DomainEvent;
import com.bcbs239.regtech.core.infrastructure.OutboxMessageEntity;
import com.bcbs239.regtech.core.infrastructure.OutboxMessageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for outbox processing beans.
 */
@Configuration
public class OutboxProcessingConfiguration {

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public Consumer<DomainEvent> dispatchDomainEventFn(DomainEventDispatcher domainEventDispatcher) {
        return domainEventDispatcher::dispatch;
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(Consumer<DomainEvent> dispatchDomainEventFn) {
        return new DomainEventPublisher(dispatchDomainEventFn);
    }

    @Bean
    public OutboxProcessor outboxProcessor(
            Consumer<DomainEvent> dispatchDomainEventFn) {
        return new OutboxProcessor(
            outboxMessageRepository,
            dispatchDomainEventFn,
            objectMapper
        );
    }
}
