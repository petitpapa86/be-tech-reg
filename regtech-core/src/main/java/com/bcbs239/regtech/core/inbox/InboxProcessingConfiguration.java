package com.bcbs239.regtech.core.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.function.Function;

/**
 * Configuration for inbox processing beans.
 */
@Configuration
public class InboxProcessingConfiguration {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn() {
        return status -> em.createQuery(
            "SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }
}