package com.bcbs239.regtech.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Generic scheduled processor for inbox events to ensure reliable event processing.
 * Processes pending and failed events at regular intervals using the InboxEventPublisher interface.
 *
 * This class can be extended by specific bounded contexts to provide their own configuration
 * and conditional processing logic.
 */
public abstract class GenericInboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GenericInboxEventProcessor.class);
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final InboxEventPublisher eventPublisher;
    private final String contextName;
    private final int maxRetries;

    protected GenericInboxEventProcessor(InboxEventPublisher eventPublisher, String contextName) {
        this(eventPublisher, contextName, DEFAULT_MAX_RETRIES);
    }

    protected GenericInboxEventProcessor(InboxEventPublisher eventPublisher, String contextName, int maxRetries) {
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
            logger.debug("Processing pending inbox events for context: {}", contextName);
            eventPublisher.processPendingEvents();
        } catch (Exception e) {
            logger.error("Error processing pending inbox events for context: {}", contextName, e);
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
            logger.debug("Retrying failed inbox events for context: {}", contextName);
            eventPublisher.retryFailedEvents(maxRetries);
        } catch (Exception e) {
            logger.error("Error retrying failed inbox events for context: {}", contextName, e);
        }
    }

    /**
     * Log inbox statistics every 5 minutes for monitoring.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void logInboxStats() {
        if (!isProcessingEnabled()) {
            return;
        }

        try {
            var stats = eventPublisher.getStats();
            logger.info("{} inbox stats - Pending: {}, Processing: {}, Processed: {}, Failed: {}, Dead Letter: {}",
                contextName, stats.pending(), stats.processing(), stats.processed(), stats.failed(), stats.deadLetter());
        } catch (Exception e) {
            logger.error("Error logging inbox statistics for context: {}", contextName, e);
        }
    }

    /**
     * Determines if inbox processing is enabled for this context.
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