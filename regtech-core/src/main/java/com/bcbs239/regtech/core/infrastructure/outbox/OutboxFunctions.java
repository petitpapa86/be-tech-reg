package com.bcbs239.regtech.core.infrastructure.outbox;

import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class OutboxFunctions {

    // Request records
    public record MarkAsProcessedRequest(String id, Instant processedAt) {}
    public record MarkAsFailedRequest(String id, String errorMessage) {}

    // Read closures
    public static Function<OutboxMessageStatus, List<OutboxMessageEntity>> findByStatusOrderByOccurredOnUtc(EntityManager em) {
        return status -> em.createQuery(
            "SELECT o FROM OutboxMessageEntity o WHERE o.status = :status ORDER BY o.occurredOnUtc ASC", OutboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }

    public static Function<OutboxMessageStatus, Long> countByStatus(EntityManager em) {
        return status -> {
            Long cnt = em.createQuery(
                "SELECT COUNT(o) FROM OutboxMessageEntity o WHERE o.status = :status", Long.class
            ).setParameter("status", status).getSingleResult();
            return cnt != null ? cnt : 0L;
        };
    }

    // Mutating closures
    public static Function<MarkAsProcessedRequest, Integer> markAsProcessed(EntityManager em, TransactionTemplate tt) {
        return req -> tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE OutboxMessageEntity o SET o.status = :processed, o.processedOnUtc = :processedAt WHERE o.id = :id"
            ).setParameter("processed", OutboxMessageStatus.PROCESSED).setParameter("processedAt", req.processedAt()).setParameter("id", req.id()).executeUpdate();
        });
    }

    public static Function<MarkAsFailedRequest, Integer> markAsFailed(EntityManager em, TransactionTemplate tt) {
        return req -> tt.execute(status -> {
            status.isNewTransaction();
            return em.createQuery(
                "UPDATE OutboxMessageEntity o SET o.status = :failed WHERE o.id = :id"
            ).setParameter("failed", OutboxMessageStatus.FAILED).setParameter("id", req.id()).executeUpdate();
        });
    }

    // Minimal CRUD
    public static Function<String, Optional<OutboxMessageEntity>> findById(EntityManager em) {
        return id -> Optional.ofNullable(em.find(OutboxMessageEntity.class, id));
    }

    public static Function<OutboxMessageEntity, OutboxMessageEntity> save(EntityManager em, TransactionTemplate tt) {
        return entity -> tt.execute(status -> {
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