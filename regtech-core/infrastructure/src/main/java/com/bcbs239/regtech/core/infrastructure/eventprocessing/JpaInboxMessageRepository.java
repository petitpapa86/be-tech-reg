package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.bcbs239.regtech.core.domain.inbox.InboxMessageStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA implementation of IInboxMessageRepository.
 * Provides domain repository functionality for inbox messages.
 */
@Repository
public class JpaInboxMessageRepository implements IInboxMessageRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final InboxMessageRepository inboxMessageRepository;

    public JpaInboxMessageRepository(InboxMessageRepository inboxMessageRepository) {
        this.inboxMessageRepository = inboxMessageRepository;
    }

    @Override
    @Transactional
    public InboxMessage save(InboxMessage message) {
        // Check for duplicate by eventId FIRST (idempotency check)
        if (message.getId() != null) {
            InboxMessageEntity existing = inboxMessageRepository.findByEventId(message.getId()).orElse(null);
            if (existing != null) {
                return toDomain(existing);
            }
        }

        InboxMessageEntity entity = toEntity(message);
        // Don't set the ID - let JPA generate it
        // The eventId field will store the original event ID for idempotency
        entity.setId(null);
        entity.setEventId(message.getId());

        try {
            InboxMessageEntity saved = inboxMessageRepository.save(entity);
            return toDomain(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Concurrent insert with same eventId - fetch existing and return it
            InboxMessageEntity existing = inboxMessageRepository.findByEventId(message.getId()).orElse(null);
            if (existing != null) {
                return toDomain(existing);
            }
            // If we can't recover, rethrow
            throw ex;
        }
    }

    @Override
    public List<InboxMessage> findByProcessingStatusOrderByReceivedAt(InboxMessageStatus inboxMessageStatus) {
        InboxMessageEntity.ProcessingStatus entityStatus = toEntityStatus(inboxMessageStatus);
        return inboxMessageRepository.findByProcessingStatusOrderByReceivedAt(entityStatus)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsProcessing(String id) {
        entityManager.createQuery(
            "UPDATE EventProcessingInboxMessageEntity i SET i.processingStatus = :processing WHERE i.eventId = :eventId AND i.processingStatus = :pending"
        )
        .setParameter("processing", InboxMessageEntity.ProcessingStatus.PROCESSING)
        .setParameter("eventId", id)
        .setParameter("pending", InboxMessageEntity.ProcessingStatus.PENDING)
        .executeUpdate();
    }

    @Override
    @Transactional
    public void markAsPermanentlyFailed(String id) {
        entityManager.createQuery(
            "UPDATE EventProcessingInboxMessageEntity i SET i.processingStatus = :failed WHERE i.eventId = :eventId"
        )
        .setParameter("failed", InboxMessageEntity.ProcessingStatus.FAILED)
        .setParameter("eventId", id)
        .executeUpdate();
    }

    @Override
    @Transactional
    public void markAsProcessed(String id, Instant now) {
        entityManager.createQuery(
            "UPDATE EventProcessingInboxMessageEntity i SET i.processingStatus = :processed, i.processedAt = :processedAt WHERE i.eventId = :eventId"
        )
        .setParameter("processed", InboxMessageEntity.ProcessingStatus.PROCESSED)
        .setParameter("processedAt", now)
        .setParameter("eventId", id)
        .executeUpdate();
    }

    private InboxMessageEntity toEntity(InboxMessage message) {
        InboxMessageEntity entity = new InboxMessageEntity();
        // Don't set ID here - it will be set in the save method
        // entity.setId() is intentionally not called
        entity.setEventId(message.getId()); // Store original event ID for idempotency
        entity.setEventType(message.getEventType());
        entity.setEventData(message.getContent());
        entity.setReceivedAt(message.getOccurredOnUtc());
        entity.setProcessedAt(message.getProcessedOnUtc());
        entity.setProcessingStatus(toEntityStatus(message.getStatus()));
        entity.setErrorMessage(message.getLastError());
        entity.setRetryCount(message.getRetryCount());
        entity.setNextRetryAt(message.getNextRetryTime());
        return entity;
    }

    private InboxMessage toDomain(InboxMessageEntity entity) {
        InboxMessage message = new InboxMessage();
        message.setId(entity.getEventId()); // Use eventId as the domain ID for consistency
        message.setEventType(entity.getEventType());
        message.setContent(entity.getEventData());
        message.setOccurredOnUtc(entity.getReceivedAt());
        message.setProcessedOnUtc(entity.getProcessedAt());
        message.setStatus(toDomainStatus(entity.getProcessingStatus()));
        message.setLastError(entity.getErrorMessage());
        message.setRetryCount(entity.getRetryCount());
        message.setNextRetryTime(entity.getNextRetryAt());
        message.setUpdatedAt(entity.getReceivedAt()); // Using receivedAt as updatedAt since entity doesn't have it
        return message;
    }

    private InboxMessageEntity.ProcessingStatus toEntityStatus(InboxMessageStatus domainStatus) {
        return switch (domainStatus) {
            case PENDING -> InboxMessageEntity.ProcessingStatus.PENDING;
            case PROCESSING -> InboxMessageEntity.ProcessingStatus.PROCESSING;
            case PROCESSED -> InboxMessageEntity.ProcessingStatus.PROCESSED;
            case FAILED -> InboxMessageEntity.ProcessingStatus.FAILED;
            case PUBLISHED, DEAD_LETTER -> InboxMessageEntity.ProcessingStatus.FAILED; // Map to FAILED as closest match
        };
    }

    private InboxMessageStatus toDomainStatus(InboxMessageEntity.ProcessingStatus entityStatus) {
        return switch (entityStatus) {
            case PENDING -> InboxMessageStatus.PENDING;
            case PROCESSING -> InboxMessageStatus.PROCESSING;
            case PROCESSED -> InboxMessageStatus.PROCESSED;
            case FAILED -> InboxMessageStatus.FAILED;
        };
    }
}