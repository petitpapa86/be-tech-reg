package com.bcbs239.regtech.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Generic scheduled processor for outbox events to ensure reliable event delivery.
 * Processes pending and failed events at regular intervals using the OutboxEventPublisher interface.
 * 
 * This class can be extended by specific bounded contexts to provide their own configuration
 * and conditional processing logic.
 */
public abstract class GenericOutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GenericOutboxEventProcessor.class);
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final OutboxEventPublisher eventPublisher;
    private final String contextName;
    private final int maxRetries;

    protected GenericOutboxEventProcessor(OutboxEventPublisher eventPublisher, String contextName) {
        this(eventPublisher, contextName, DEFAULT_MAX_RETRIES);
    }

    protected GenericOutboxEventProcessor(OutboxEventPublisher eventPublisher, String contextName, int maxRetries) {
        this.eventPublisher = eventPublisher;
        this.contextName = contextName;
        this.maxRetries = maxRetries;
    }

    /**
     * Process pending events every 30 seconds.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processPendingEvents() {
        if (!isProcessingEnabled()) {
            return;
        }

        try {
            logger.debug("Processing pending outbox events for context: {}", contextName);
            eventPublisher.processPendingEvents();
        } catch (Exception e) {
            logger.error("Error processing pending outbox events for context: {}", contextName, e);
        }
    }

    /**
     * Retry failed events every 2 minutes.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    public void retryFailedEvents() {
        if (!isProcessingEnabled()) {
            return;
        }

        try {
            logger.debug("Retrying failed outbox events for context: {}", contextName);
            eventPublisher.retryFailedEvents(maxRetries);
        } catch (Exception e) {
            logger.error("Error retrying failed outbox events for context: {}", contextName, e);
        }
    }

    /**
     * Log outbox statistics every 5 minutes for monitoring.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void logOutboxStats() {
        if (!isProcessingEnabled()) {
            return;
        }

        try {
            var stats = eventPublisher.getStats();
            logger.info("{} outbox stats - Pending: {}, Processing: {}, Processed: {}, Failed: {}, Dead Letter: {}",
                contextName, stats.pending(), stats.processing(), stats.processed(), stats.failed(), stats.deadLetter());
        } catch (Exception e) {
            logger.error("Error logging outbox statistics for context: {}", contextName, e);
        }
    }

    /**
     * Determines if outbox processing is enabled for this context.
     * Subclasses should override this method to provide context-specific configuration.
     * 
     * @return true if processing is enabled, false otherwise
     */
    protected abstract boolean isProcessingEnabled();

    /**
     * Get the context name for logging and monitoring.
     * 
     * @return The context name
     */
    protected String getContextName() {
        return contextName;
    }

    /**
     * Get the maximum number of retries for failed events.
     * 
     * @return The maximum retry count
     */
    protected int getMaxRetries() {
        return maxRetries;
    }
}