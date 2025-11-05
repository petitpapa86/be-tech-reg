package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class InboxFunctions {

    // Request records
    public record MarkAsProcessingRequest(String id) {}
    public record MarkAsProcessedRequest(String id, Instant processedAt) {}
    public record MarkAsFailedWithRetryRequest(String id, String errorMessage, Instant nextRetryAt) {}
    public record MarkAsPermanentlyFailedRequest(String id, String errorMessage) {}
    public record DeleteProcessedMessagesRequest(Instant cutoffDate) {}

    // Read operations
    public static List<InboxMessageEntity> findPendingMessages(EntityManager em, InboxMessageEntity.ProcessingStatus status) {
        return em.createQuery(
            "SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :status ORDER BY i.receivedAt ASC", InboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }

    public static List<InboxMessageEntity> findFailedMessagesEligibleForRetry(EntityManager em, Instant now) {
        return em.createQuery(
            "SELECT i FROM InboxMessageEntity i WHERE i.processingStatus = :failed AND i.nextRetryAt <= :now ORDER BY i.nextRetryAt ASC", InboxMessageEntity.class
        ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("now", now).getResultList();
    }

    public static Long countByProcessingStatus(EntityManager em, InboxMessageEntity.ProcessingStatus status) {
        Long cnt = em.createQuery(
            "SELECT COUNT(i) FROM InboxMessageEntity i WHERE i.processingStatus = :status", Long.class
        ).setParameter("status", status).getSingleResult();
        return cnt != null ? cnt : 0L;
    }

    // Mutating operations
    public static Integer markAsProcessing(EntityManager em, TransactionTemplate tt, MarkAsProcessingRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :processing WHERE i.id = :id AND i.processingStatus = :pending"
            ).setParameter("processing", InboxMessageEntity.ProcessingStatus.PROCESSING).setParameter("id", req.id()).setParameter("pending", InboxMessageEntity.ProcessingStatus.PENDING).executeUpdate();
        });
    }

    public static Integer markAsProcessed(EntityManager em, TransactionTemplate tt, MarkAsProcessedRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :processed, i.processedAt = :processedAt WHERE i.id = :id"
            ).setParameter("processed", InboxMessageEntity.ProcessingStatus.PROCESSED).setParameter("processedAt", req.processedAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    public static Integer markAsFailedWithRetry(EntityManager em, TransactionTemplate tt, MarkAsFailedWithRetryRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :failed, i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1, i.nextRetryAt = :nextRetryAt WHERE i.id = :id"
            ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("errorMessage", req.errorMessage()).setParameter("nextRetryAt", req.nextRetryAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    public static Integer markAsPermanentlyFailed(EntityManager em, TransactionTemplate tt, MarkAsPermanentlyFailedRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE InboxMessageEntity i SET i.processingStatus = :failed, i.errorMessage = :errorMessage, i.retryCount = i.retryCount + 1 WHERE i.id = :id"
            ).setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED).setParameter("errorMessage", req.errorMessage()).setParameter("id", req.id()).executeUpdate();
        });
    }

    public static Integer deleteProcessedMessagesOlderThan(EntityManager em, TransactionTemplate tt, DeleteProcessedMessagesRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "DELETE FROM InboxMessageEntity i WHERE i.processingStatus = :processed AND i.processedAt < :cutoffDate"
            ).setParameter("processed", InboxMessageEntity.ProcessingStatus.PROCESSED).setParameter("cutoffDate", req.cutoffDate()).executeUpdate();
        });
    }

    // Minimal CRUD
    public static Optional<InboxMessageEntity> findById(EntityManager em, String id) {
        return Optional.ofNullable(em.find(InboxMessageEntity.class, id));
    }

    public static InboxMessageEntity save(EntityManager em, TransactionTemplate tt, InboxMessageEntity entity) {
        return tt.execute(status -> {
            status.isNewTransaction();
            // Idempotency: if eventId is present, check for existing message with same eventId
            if (entity.getEventId() != null && !entity.getEventId().isBlank()) {
                var existing = em.createQuery(
                    "SELECT i FROM InboxMessageEntity i WHERE i.eventId = :eventId", InboxMessageEntity.class
                ).setParameter("eventId", entity.getEventId()).getResultList();

                if (existing != null && !existing.isEmpty()) {
                    // Return existing record (do not create duplicate)
                    return existing.get(0);
                }
            }

            if (entity.getId() == null || em.find(InboxMessageEntity.class, entity.getId()) == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        });
    }
}
