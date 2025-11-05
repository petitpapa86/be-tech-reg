package com.bcbs239.regtech.core.domain.outbox;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for outbox messages.
 */
public interface IOutboxMessageRepository {

    OutboxMessage save(OutboxMessage message);

    Optional<OutboxMessage> findById(String id);

    List<OutboxMessage> findPendingMessages();

    List<OutboxMessage> findFailedMessages();

    void deleteById(String id);

    List<OutboxMessage> findByStatusOrderByOccurredOnUtc(OutboxMessageStatus outboxMessageStatus);
}

