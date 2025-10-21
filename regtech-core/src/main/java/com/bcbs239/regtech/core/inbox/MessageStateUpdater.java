package com.bcbs239.regtech.core.inbox;

/**
 * @deprecated Use the functional beans `Consumer<String> markAsProcessedFn` and
 * `BiConsumer<String,String> markAsPermanentlyFailedFn` provided by
 * `FunctionalMessageStateConfig` instead of this interface.
 */
@Deprecated
public interface MessageStateUpdater {
    void markAsProcessed(String messageId);
    void markAsPermanentlyFailed(String messageId, String reason);
}
