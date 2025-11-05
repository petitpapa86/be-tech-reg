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


    @Override
    public OutboxMessage save(OutboxMessage message) {
        return null;
    }

    @Override
    public Optional<OutboxMessage> findById(String id) {
        return Optional.empty();
    }

    @Override
    public List<OutboxMessage> findPendingMessages() {
        return List.of();
    }

    @Override
    public List<OutboxMessage> findFailedMessages() {
        return List.of();
    }

    @Override
    public void deleteById(String id) {

    }

    @Override
    public List<OutboxMessage> findByStatusOrderByOccurredOnUtc(OutboxMessageStatus outboxMessageStatus) {
        return List.of();
    }
}
