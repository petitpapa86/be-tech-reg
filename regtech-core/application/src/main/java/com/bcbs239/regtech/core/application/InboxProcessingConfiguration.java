package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.infrastructure.InboxFunctions;
import com.bcbs239.regtech.core.infrastructure.InboxMessageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        return status -> InboxFunctions.findPendingMessages(em, status);
    }

    @Bean
    public Function<Instant, List<InboxMessageEntity>> findFailedMessagesEligibleForRetryFn() {
        return now -> InboxFunctions.findFailedMessagesEligibleForRetry(em, now);
    }

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, Long> countByProcessingStatusFn() {
        return status -> InboxFunctions.countByProcessingStatus(em, status);
    }

    @Bean
    public Function<InboxFunctions.MarkAsProcessingRequest, Integer> markAsProcessingCoreFn() {
        return req -> InboxFunctions.markAsProcessing(em, transactionTemplate, req);
    }

    @Bean
    public Function<InboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedCoreFn() {
        return req -> InboxFunctions.markAsProcessed(em, transactionTemplate, req);
    }

    @Bean
    public Function<InboxFunctions.MarkAsFailedWithRetryRequest, Integer> markAsFailedWithRetryFn() {
        return req -> InboxFunctions.markAsFailedWithRetry(em, transactionTemplate, req);
    }

    @Bean
    public Function<InboxFunctions.MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedCoreFn() {
        return req -> InboxFunctions.markAsPermanentlyFailed(em, transactionTemplate, req);
    }

    @Bean
    public Function<InboxFunctions.DeleteProcessedMessagesRequest, Integer> deleteProcessedMessagesOlderThanFn() {
        return req -> InboxFunctions.deleteProcessedMessagesOlderThan(em, transactionTemplate, req);
    }

    @Bean
    public Function<String, Optional<InboxMessageEntity>> findByIdFn() {
        return id -> InboxFunctions.findById(em, id);
    }

    @Bean
    public Function<InboxMessageEntity, InboxMessageEntity> saveFn() {
        return entity -> InboxFunctions.save(em, transactionTemplate, entity);
    }
}
