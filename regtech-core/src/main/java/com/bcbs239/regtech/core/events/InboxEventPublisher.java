package com.bcbs239.regtech.core.events;

/**
 * Generic interface for inbox event publishers.
 * Provides the contract for reliable event processing across bounded contexts.
 */
public interface InboxEventPublisher {

    /**
     * Process pending events from the inbox.
     */
    void processPendingEvents();

    /**
     * Retry failed events up to the specified maximum retry count.
     *
     * @param maxRetries Maximum number of retries for failed events
     */
    void retryFailedEvents(int maxRetries);

    /**
     * Get statistics about event processing for monitoring.
     *
     * @return Event processing statistics
     */
    InboxEventStats getStats();

    /**
     * Statistics record for inbox event processing monitoring.
     */
    record InboxEventStats(
        long pending,
        long processing,
        long processed,
        long failed,
        long deadLetter
    ) {}
}