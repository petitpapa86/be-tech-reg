package com.bcbs239.regtech.core.infrastructure.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for outbox messages using EntityManager.
 */
@Repository
public class OutboxMessageRepository {

    @PersistenceContext
    private EntityManager em;

    public List<OutboxMessageEntity> findByStatusOrderByOccurredOnUtc(OutboxMessageStatus status) {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC",
            OutboxMessageEntity.class
        ).setParameter("status", status).getResultList();
    }

    public long countByStatus(OutboxMessageStatus status) {
        Long count = em.createQuery(
            "SELECT COUNT(om) FROM OutboxMessageEntity om WHERE om.status = :status",
            Long.class
        ).setParameter("status", status).getSingleResult();
        return count != null ? count : 0L;
    }

    public OutboxMessageEntity save(OutboxMessageEntity entity) {
        if (entity.getId() == null || em.find(OutboxMessageEntity.class, entity.getId()) == null) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }

    public List<OutboxMessageEntity> findAll() {
        return em.createQuery("SELECT om FROM OutboxMessageEntity om", OutboxMessageEntity.class).getResultList();
    }
}