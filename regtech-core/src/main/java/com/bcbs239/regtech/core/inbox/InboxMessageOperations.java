package com.bcbs239.regtech.core.inbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository exposing only functional closures for inbox message persistence operations.
 * All operations are available as java.util.function closures; there are no public
 * direct mutating methods on this class anymore (caller should use the closures).
 */
@Repository
public class InboxMessageOperations {

    @SuppressWarnings("unused")
    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unused")
    @Autowired
    private TransactionTemplate transactionTemplate;

    // --- Request records for typed closures ---

    public record MarkAsProcessingRequest(String id) {}
    public record MarkAsProcessedRequest(String id, Instant processedAt) {}
    public record MarkAsFailedWithRetryRequest(String id, String errorMessage, Instant nextRetryAt) {}
    public record MarkAsPermanentlyFailedRequest(String id, String errorMessage) {}
    public record DeleteProcessedMessagesRequest(Instant cutoffDate) {}

    // --- Functional wrappers (closures) only ---

    // Read closures (no explicit transaction needed)

    @SuppressWarnings("unused")
    public Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> findPendingMessagesFn() {
        return status -> em.createQuery(
            "SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }

    @SuppressWarnings("unused")
    public Function<Instant, List<InboxMessageEntity>> findFailedMessagesEligibleForRetryFn() {
        return now -> em.createQuery(
            "SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :failed AND i.nextRetryAt <= :now ORDER BY i.nextRetryAt ASC", InboxMessageEntity.class
        ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("now", now).getResultList();
    }

    @SuppressWarnings("unused")
    public Function<InboxMessageEntity.ProcessingStatus, Long> countByProcessingStatusFn() {
        return status -> {
            Long cnt = em.createQuery(
                "SELECT COUNT(i) FROM InboxMessageEntity i WHERE i.processingStatus = :status", Long.class
            ).setParameter("status", status).getSingleResult();
            return cnt != null ? cnt : 0L;
        };
    }

    // Mutating closures: use transactionTemplate to ensure transactional boundary when invoked

    @SuppressWarnings("unused")
    public Function<MarkAsProcessingRequest, Integer> markAsProcessingFn() {
        return req -> transactionTemplate.execute(status -> {
            // touch status to avoid unused-parameter warnings from static analyzers
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :processing WHERE i.id = :id AND i.processingStatus = :pending"
            ).setParameter("processing", InboxMessageEntity.ProcessingStatus.PROCESSING).setParameter("id", req.id()).setParameter("pending", InboxMessageEntity.ProcessingStatus.PENDING).executeUpdate();
        });
    }

    @SuppressWarnings("unused")
    public Function<MarkAsProcessedRequest, Integer> markAsProcessedFn() {
        return req -> transactionTemplate.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :processed, i.processedAt = :processedAt WHERE i.id = :id"
            ).setParameter("processed", InboxMessageEntity.ProcessingStatus.PROCESSED).setParameter("processedAt", req.processedAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    @SuppressWarnings("unused")
    public Function<MarkAsFailedWithRetryRequest, Integer> markAsFailedWithRetryFn() {
        return req -> transactionTemplate.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :failed, i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1, i.nextRetryAt = :nextRetryAt WHERE i.id = :id"
            ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("errorMessage", req.errorMessage()).setParameter("nextRetryAt", req.nextRetryAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    @SuppressWarnings("unused")
    public Function<MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedFn() {
        return req -> transactionTemplate.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :failed, i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1 WHERE i.id = :id"
            ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("errorMessage", req.errorMessage()).setParameter("id", req.id()).executeUpdate();
        });
    }

    @SuppressWarnings("unused")
    public Function<DeleteProcessedMessagesRequest, Integer> deleteProcessedMessagesOlderThanFn() {
        return req -> transactionTemplate.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "DELETE FROM InboxMessageEntity i WHERE i.processingStatus = :processed AND i.processedAt < :cutoffDate"
            ).setParameter("processed", InboxMessageEntity.ProcessingStatus.PROCESSED).setParameter("cutoffDate", req.cutoffDate()).executeUpdate();
        });
    }

    // Provide minimal CRUD helpers so code that previously used JpaRepository.save/findById keeps working
    public java.util.Optional<InboxMessageEntity> findById(String id) {
        return java.util.Optional.ofNullable(em.find(InboxMessageEntity.class, id));
    }

    public InboxMessageEntity save(InboxMessageEntity entity) {
        return transactionTemplate.execute(status -> {
            // touch status to avoid unused-parameter warnings
            status.isNewTransaction();
            if (entity.getId() == null || em.find(InboxMessageEntity.class, entity.getId()) == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        });
    }
}
