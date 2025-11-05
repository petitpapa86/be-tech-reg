package com.bcbs239.regtech.core.infrastructure.eventprocessing;

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

    public java.util.Optional<OutboxMessageEntity> findById(String id) {
        return java.util.Optional.ofNullable(em.find(OutboxMessageEntity.class, id));
    }

    public List<OutboxMessageEntity> findRetryableMessages(java.time.Instant now) {
        List<OutboxMessageEntity> entities = em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND " +
            "(om.nextRetryTime IS NULL OR om.nextRetryTime <= :now)",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .setParameter("now", now)
         .getResultList();

        return entities;
    }

    public List<OutboxMessageEntity> findStuckMessages(java.time.Instant cutoffTime) {
        List<OutboxMessageEntity> entities = em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND om.updatedAt < :cutoffTime",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .setParameter("cutoffTime", cutoffTime)
         .getResultList();

        return entities;
    }

    public List<OutboxMessageEntity> findOldDeadLetterMessages(java.time.Instant cutoffTime) {
        List<OutboxMessageEntity> entities = em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND om.deadLetterTime < :cutoffTime",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.DEAD_LETTER)
         .setParameter("cutoffTime", cutoffTime)
         .getResultList();

        return entities;
    }

    public long countStuckMessages(java.time.Instant cutoffTime) {
        Long count = em.createQuery(
            "SELECT COUNT(om) FROM OutboxMessageEntity om WHERE om.status = :status AND om.updatedAt < :cutoffTime",
            Long.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .setParameter("cutoffTime", cutoffTime)
         .getSingleResult();
        return count != null ? count : 0L;
    }

    public void delete(OutboxMessageEntity entity) {
        if (em.contains(entity)) {
            em.remove(entity);
        } else {
            OutboxMessageEntity managedEntity = em.find(OutboxMessageEntity.class, entity.getId());
            if (managedEntity != null) {
                em.remove(managedEntity);
            }
        }
    }
}
