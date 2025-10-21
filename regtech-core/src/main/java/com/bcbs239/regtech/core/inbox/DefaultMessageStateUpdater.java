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

    private final InboxMessageJpaRepository repository;

    public DefaultMessageStateUpdater(InboxMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void markAsProcessed(String messageId) {
        repository.markAsProcessed(messageId, Instant.now());
    }

    @Override
    public void markAsPermanentlyFailed(String messageId, String reason) {
        repository.markAsPermanentlyFailed(messageId, reason);
    }
}
