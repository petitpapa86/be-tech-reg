package com.bcbs239.regtech.core.inbox;

import java.time.Instant;

/**
 * @deprecated Prefer functional beans `Consumer<String> markAsProcessedFn` and
 * `BiConsumer<String,String> markAsPermanentlyFailedFn` declared in
 * `FunctionalMessageStateConfig`. This class is kept for source-compatibility
 * but is no longer a Spring component.
 */
@Deprecated
public class DefaultMessageStateUpdater implements MessageStateUpdater {

    private final InboxMessageOperations repository;

    public DefaultMessageStateUpdater(InboxMessageOperations repository) {
        this.repository = repository;
    }

    @Override
    public void markAsProcessed(String messageId) {
        repository.markAsProcessedFn().apply(new InboxMessageOperations.MarkAsProcessedRequest(messageId, Instant.now()));
    }

    @Override
    public void markAsPermanentlyFailed(String messageId, String reason) {
        repository.markAsPermanentlyFailedFn().apply(new InboxMessageOperations.MarkAsPermanentlyFailedRequest(messageId, reason));
    }
}
