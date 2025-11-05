package com.bcbs239.regtech.core.domain.eventprocessing;

import java.util.List;

/**
 * Domain repository interface for inbox message consumers.
 */
public interface IInboxMessageConsumerRepository {

    InboxMessageConsumer save(InboxMessageConsumer consumer);

    List<InboxMessageConsumer> findByInboxMessageId(String inboxMessageId);

    boolean existsByInboxMessageIdAndName(String inboxMessageId, String name);

    void deleteById(Long id);
}