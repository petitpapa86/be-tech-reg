package com.bcbs239.regtech.core.inbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Configuration for inbox processing beans.
 */
@Configuration
public class InboxProcessingConfiguration {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn() {
        return InboxFunctions.findPendingMessages(em);
    }

    @Bean
    public Function<Instant, List<InboxMessageEntity>> findFailedMessagesEligibleForRetryFn() {
        return InboxFunctions.findFailedMessagesEligibleForRetry(em);
    }

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, Long> countByProcessingStatusFn() {
        return InboxFunctions.countByProcessingStatus(em);
    }

    @Bean
    public Function<InboxFunctions.MarkAsProcessingRequest, Integer> markAsProcessingFn() {
        return InboxFunctions.markAsProcessing(em, transactionTemplate);
    }

    @Bean
    public Function<InboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedCoreFn() {
        return InboxFunctions.markAsProcessed(em, transactionTemplate);
    }

    @Bean
    public Function<InboxFunctions.MarkAsFailedWithRetryRequest, Integer> markAsFailedWithRetryFn() {
        return InboxFunctions.markAsFailedWithRetry(em, transactionTemplate);
    }

    @Bean
    public Function<InboxFunctions.MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedCoreFn() {
        return InboxFunctions.markAsPermanentlyFailed(em, transactionTemplate);
    }

    @Bean
    public Function<InboxFunctions.DeleteProcessedMessagesRequest, Integer> deleteProcessedMessagesOlderThanFn() {
        return InboxFunctions.deleteProcessedMessagesOlderThan(em, transactionTemplate);
    }

    @Bean
    public Function<String, Optional<InboxMessageEntity>> findByIdFn() {
        return InboxFunctions.findById(em);
    }

    @Bean
    public Function<InboxMessageEntity, InboxMessageEntity> saveFn() {
        return InboxFunctions.save(em, transactionTemplate);
    }
}