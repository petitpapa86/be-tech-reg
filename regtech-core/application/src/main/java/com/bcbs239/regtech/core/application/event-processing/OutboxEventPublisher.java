package com.bcbs239.regtech.core.application.eventprocessing;

/**
 * Generic interface for outbox event publishers.
 * Provides the contract for reliable event processing across bounded contexts.
 */
public interface OutboxEventPublisher {
    
    /**
     * Process pending events from the outbox.
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
    OutboxEventStats getStats();
    
    /**
     * Statistics record for outbox event processing monitoring.
     */
    record OutboxEventStats(
        long pending,
        long processing,
        long processed,
        long failed,
        long deadLetter
    ) {}
}
