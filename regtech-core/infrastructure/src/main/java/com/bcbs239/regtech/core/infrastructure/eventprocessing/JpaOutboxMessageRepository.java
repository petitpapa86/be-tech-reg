package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.outbox.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessage;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessageStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of IOutboxMessageRepository.
 * Provides domain repository functionality for outbox messages.
 */
@Repository
public class JpaOutboxMessageRepository implements IOutboxMessageRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final OutboxMessageRepository outboxMessageRepository;

    public JpaOutboxMessageRepository(OutboxMessageRepository outboxMessageRepository) {
        this.outboxMessageRepository = outboxMessageRepository;
    }

    @Override
    @Transactional
    public OutboxMessage save(OutboxMessage message) {
        OutboxMessageEntity entity = toEntity(message);
        OutboxMessageEntity saved = outboxMessageRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<OutboxMessage> findById(String id) {
        return outboxMessageRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<OutboxMessage> findPendingMessages() {
        return outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxMessage> findFailedMessages() {
        return outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.FAILED)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        outboxMessageRepository.deleteById(id);
    }

    @Override
    public List<OutboxMessage> findByStatusOrderByOccurredOnUtc(OutboxMessageStatus outboxMessageStatus) {
        OutboxMessageStatus entityStatus = toEntityStatus(outboxMessageStatus);
        return outboxMessageRepository.findByStatusOrderByOccurredOnUtc(entityStatus)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private OutboxMessageEntity toEntity(OutboxMessage message) {
        OutboxMessageEntity entity = new OutboxMessageEntity();
        entity.setId(message.getId());
        entity.setType(message.getType());
        entity.setContent(message.getContent());
        entity.setStatus(toEntityStatus(message.getStatus()));
        entity.setOccurredOnUtc(message.getOccurredOnUtc());
        entity.setProcessedOnUtc(message.getProcessedOnUtc());
        entity.setRetryCount(message.getRetryCount());
        entity.setNextRetryTime(message.getNextRetryTime());
        entity.setLastError(message.getLastError());
        entity.setDeadLetterTime(message.getDeadLetterTime());
        entity.setUpdatedAt(message.getUpdatedAt());
        return entity;
    }

    private OutboxMessage toDomain(OutboxMessageEntity entity) {
        OutboxMessage message = new OutboxMessage(entity.getType(), entity.getContent(), entity.getOccurredOnUtc());
        message.setId(entity.getId());
        message.setStatus(toDomainStatus(entity.getStatus()));
        message.setProcessedOnUtc(entity.getProcessedOnUtc());
        message.setRetryCount(entity.getRetryCount());
        message.setNextRetryTime(entity.getNextRetryTime());
        message.setLastError(entity.getLastError());
        message.setDeadLetterTime(entity.getDeadLetterTime());
        message.setUpdatedAt(entity.getUpdatedAt());
        return message;
    }

    private OutboxMessageStatus toEntityStatus(OutboxMessageStatus domainStatus) {
        return domainStatus; // They are the same enum
    }

    private OutboxMessageStatus toDomainStatus(OutboxMessageStatus entityStatus) {
        return entityStatus; // They are the same enum
    }
}