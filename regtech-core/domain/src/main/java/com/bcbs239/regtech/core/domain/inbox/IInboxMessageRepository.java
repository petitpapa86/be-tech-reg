package com.bcbs239.regtech.core.domain.inbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for inbox messages.
 */
public interface IInboxMessageRepository {

    InboxMessage save(InboxMessage message);

    List<InboxMessage> findByProcessingStatusOrderByReceivedAt(InboxMessageStatus inboxMessageStatus);

    void markAsProcessing(String id);

    void markAsPermanentlyFailed(String id);

    void markAsProcessed(String id, Instant now);
}