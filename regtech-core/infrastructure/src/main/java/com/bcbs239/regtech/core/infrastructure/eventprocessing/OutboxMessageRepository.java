package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.outbox.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessage;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessageStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for outbox messages using EntityManager.
 * Implements domain port `IOutboxMessageRepository` and returns domain `OutboxMessage`.
 */
@Repository
public class OutboxMessageRepository implements IOutboxMessageRepository {

    @PersistenceContext
    private EntityManager em;

    // Domain port methods
    @Override
    public OutboxMessage save(OutboxMessage message) {
        if (message instanceof OutboxMessageEntity entity) {
            if (entity.getId() == null || em.find(OutboxMessageEntity.class, entity.getId()) == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        }
        throw new IllegalArgumentException("Unsupported OutboxMessage implementation: " + message.getClass().getName());
    }

    @Override
    public Optional<OutboxMessage> findById(String id) {
        return Optional.ofNullable(em.find(OutboxMessageEntity.class, id));
    }

    @Override
    public List<OutboxMessage> findPendingMessages() {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .getResultList()
         .stream().map(e -> (OutboxMessage) e).collect(Collectors.toList());
    }

    @Override
    public List<OutboxMessage> findFailedMessages() {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.FAILED)
         .getResultList()
         .stream().map(e -> (OutboxMessage) e).collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        OutboxMessageEntity entity = em.find(OutboxMessageEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    // Existing infra-specific operations (kept for adapters/jobs)
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

    public List<OutboxMessageEntity> findAll() {
        return em.createQuery("SELECT om FROM OutboxMessageEntity om", OutboxMessageEntity.class).getResultList();
    }

    public Optional<OutboxMessageEntity> findEntityById(String id) {
        return Optional.ofNullable(em.find(OutboxMessageEntity.class, id));
    }

    public List<OutboxMessageEntity> findRetryableMessages(java.time.Instant now) {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND " +
            "(om.nextRetryTime IS NULL OR om.nextRetryTime <= :now)",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .setParameter("now", now)
         .getResultList();
    }

    public List<OutboxMessageEntity> findStuckMessages(java.time.Instant cutoffTime) {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND om.updatedAt < :cutoffTime",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING)
         .setParameter("cutoffTime", cutoffTime)
         .getResultList();
    }

    public List<OutboxMessageEntity> findOldDeadLetterMessages(java.time.Instant cutoffTime) {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status AND om.deadLetterTime < :cutoffTime",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.DEAD_LETTER)
         .setParameter("cutoffTime", cutoffTime)
         .getResultList();
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
