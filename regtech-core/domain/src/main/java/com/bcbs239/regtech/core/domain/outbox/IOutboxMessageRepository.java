package com.bcbs239.regtech.core.domain.outbox;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for outbox messages.
 */
public interface IOutboxMessageRepository {

    OutboxMessage save(OutboxMessage message);
    
    /**
     * Persist a batch of outbox messages in a single operation and return the saved messages.
     */
    java.util.List<OutboxMessage> saveAll(java.util.List<OutboxMessage> messages);

    Optional<OutboxMessage> findById(String id);

    List<OutboxMessage> findPendingMessages();

    List<OutboxMessage> findFailedMessages();

    void deleteById(String id);

    List<OutboxMessage> findByStatusOrderByOccurredOnUtc(OutboxMessageStatus outboxMessageStatus);
}

