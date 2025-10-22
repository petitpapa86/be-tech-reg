package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration for outbox processing beans.
 */
@Configuration
public class OutboxProcessingConfiguration {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public Function<OutboxMessageStatus, List<OutboxMessageEntity>> findPendingOutboxMessagesFn() {
        return OutboxFunctions.findByStatusOrderByOccurredOnUtc(em);
    }

    @Bean
    public Function<OutboxMessageStatus, Long> countOutboxByStatusFn() {
        return OutboxFunctions.countByStatus(em);
    }

    @Bean
    public Function<OutboxFunctions.MarkAsProcessedRequest, Integer> markOutboxAsProcessedFn() {
        return OutboxFunctions.markAsProcessed(em, transactionTemplate);
    }

    @Bean
    public Function<OutboxFunctions.MarkAsFailedRequest, Integer> markOutboxAsFailedFn() {
        return OutboxFunctions.markAsFailed(em, transactionTemplate);
    }

    @Bean
    public Function<String, Optional<OutboxMessageEntity>> findOutboxByIdFn() {
        return OutboxFunctions.findById(em);
    }

    @Bean
    public Function<OutboxMessageEntity, OutboxMessageEntity> saveOutboxFn() {
        return OutboxFunctions.save(em, transactionTemplate);
    }

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
            Function<OutboxMessageStatus, List<OutboxMessageEntity>> findPendingOutboxMessagesFn,
            Consumer<String> markAsProcessedOutboxFn,
            BiConsumer<String, String> markAsFailedFn,
            Consumer<DomainEvent> dispatchDomainEventFn) {
        return new OutboxProcessor(
            findPendingOutboxMessagesFn,
            markAsProcessedOutboxFn,
            markAsFailedFn,
            dispatchDomainEventFn,
            objectMapper
        );
    }
}