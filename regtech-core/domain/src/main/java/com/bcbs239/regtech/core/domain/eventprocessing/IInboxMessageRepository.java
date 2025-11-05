package com.bcbs239.regtech.core.domain.eventprocessing;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for inbox messages.
 */
public interface IInboxMessageRepository {

    InboxMessage save(InboxMessage message);

    Optional<InboxMessage> findById(String id);

    Optional<InboxMessage> findByEventId(String eventId);

    List<InboxMessage> findPendingMessages();

    List<InboxMessage> findByAggregateId(String aggregateId);

    void deleteById(String id);
}