package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.infrastructure.OutboxMessageEntity;
import com.bcbs239.regtech.core.infrastructure.OutboxMessageStatus;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class OutboxFunctions {

    // Request records
    public record MarkAsProcessedRequest(String id, Instant processedAt) {}
    public record MarkAsFailedRequest(String id, String errorMessage) {}

    // Read operations
    public static List<OutboxMessageEntity> findByStatusOrderByOccurredOnUtc(EntityManager em, OutboxMessageStatus status) {
        return em.createQuery(
            "SELECT o FROM OutboxMessageEntity o WHERE o.status = :status ORDER BY o.occurredOnUtc ASC", OutboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }

    public static Long countByStatus(EntityManager em, OutboxMessageStatus status) {
        Long cnt = em.createQuery(
            "SELECT COUNT(o) FROM OutboxMessageEntity o WHERE o.status = :status", Long.class
        ).setParameter("status", status).getSingleResult();
        return cnt != null ? cnt : 0L;
    }

    // Mutating operations
    public static Integer markAsProcessed(EntityManager em, TransactionTemplate tt, MarkAsProcessedRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE OutboxMessageEntity o SET o.status = :processed, o.processedOnUtc = :processedAt WHERE o.id = :id"
            ).setParameter("processed", OutboxMessageStatus.PROCESSED).setParameter("processedAt", req.processedAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    public static Integer markAsFailed(EntityManager em, TransactionTemplate tt, MarkAsFailedRequest req) {
        return tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE OutboxMessageEntity o SET o.status = :failed WHERE o.id = :id"
            ).setParameter("failed", OutboxMessageStatus.FAILED).setParameter("id", req.id()).executeUpdate();
        });
    }

    // Minimal CRUD
    public static Optional<OutboxMessageEntity> findById(EntityManager em, String id) {
        return Optional.ofNullable(em.find(OutboxMessageEntity.class, id));
    }

    public static OutboxMessageEntity save(EntityManager em, TransactionTemplate tt, OutboxMessageEntity entity) {
        return tt.execute(status -> {
            status.isNewTransaction();
            if (entity.getId() == null || em.find(OutboxMessageEntity.class, entity.getId()) == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        });
    }
}
