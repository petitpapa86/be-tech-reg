package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing inbox message consumers.
 * Provides methods to check if a handler has already processed a message.
 */
@Repository
public interface InboxMessageConsumerRepository extends JpaRepository<InboxMessageConsumer, Long> {

    /**
     * Checks if a consumer with the given name has already processed the message.
     *
     * @param inboxMessageId The ID of the inbox message
     * @param name The name of the consumer/handler
     * @return true if the consumer has processed the message, false otherwise
     */
    boolean existsByInboxMessageIdAndName(String inboxMessageId, String name);
}
