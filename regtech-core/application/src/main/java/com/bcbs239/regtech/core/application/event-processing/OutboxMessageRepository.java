package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage;
import com.bcbs239.regtech.core.domain.events.OutboxMessageStatus;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Application repository for outbox messages implementing domain interface.
 */
// @Repository // Moved implementation to infrastructure module to satisfy Clean Architecture
public class OutboxMessageRepository implements IOutboxMessageRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public OutboxMessage save(OutboxMessage message) {
        if (message instanceof OutboxMessageEntity) {
            OutboxMessageEntity entity = (OutboxMessageEntity) message;
            if (entity.getId() == null || em.find(OutboxMessageEntity.class, entity.getId()) == null) {
                em.persist(entity);
                return entity;
            } else {
                return em.merge(entity);
            }
        }
        throw new IllegalArgumentException("Unsupported OutboxMessage implementation");
    }

    @Override
    public Optional<OutboxMessage> findById(String id) {
        OutboxMessageEntity entity = em.find(OutboxMessageEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<OutboxMessage> findPendingMessages() {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.PENDING).getResultList();
    }

    @Override
    public List<OutboxMessage> findFailedMessages() {
        return em.createQuery(
            "SELECT om FROM OutboxMessageEntity om WHERE om.status = :status ORDER BY om.occurredOnUtc ASC",
            OutboxMessageEntity.class
        ).setParameter("status", OutboxMessageStatus.FAILED).getResultList();
    }

    @Override
    public void deleteById(String id) {
        OutboxMessageEntity entity = em.find(OutboxMessageEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
}
