package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
     * Process pending events every 30 seconds asynchronously.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @Async
    @Transactional
    public void processPendingEvents() {
        if (!isProcessingEnabled()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put("module", contextName);
        MDC.put("processor", "outbox");

        try {
            logger.info("Starting outbox event processing", LoggingConfiguration.createStructuredLog("OUTBOX_PROCESSING_START", Map.of(
                "context", contextName,
                "operation", "process_pending"
            )));

            // Get stats before processing
            OutboxEventPublisher.OutboxEventStats beforeStats = eventPublisher.getStats();

            // Process pending events
            eventPublisher.processPendingEvents();

            // Get stats after processing to calculate how many were processed
            OutboxEventPublisher.OutboxEventStats afterStats = eventPublisher.getStats();
            long processed = afterStats.processed() - beforeStats.processed();

            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logBatchProcessing("outbox", contextName, 0, (int) processed, 0, duration);

            logger.info("Completed outbox event processing",
                    LoggingConfiguration.createStructuredLog("OUTBOX_PROCESSING_COMPLETED", Map.of(
                            "context", contextName,
                            "processed", processed,
                            "duration", duration
                    )));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logError("outbox_processing", "PROCESSING_FAILED", e.getMessage(), e, Map.of(
                "context", contextName,
                "duration", duration
            ));

            logger.error("Error processing pending outbox events", LoggingConfiguration.createStructuredLog("OUTBOX_PROCESSING_FAILED", Map.of(
                "context", contextName,
                "error", e.getMessage(),
                "duration", duration
            )), e);
        } finally {
            MDC.remove("module");
            MDC.remove("processor");
        }
    }

    /**
     * Retry failed events every 2 minutes asynchronously.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    @Async
    @Transactional
    public void retryFailedEvents() {
        if (!isProcessingEnabled()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        MDC.put("module", contextName);
        MDC.put("processor", "outbox");

        try {
            logger.info("Starting outbox event retry", LoggingConfiguration.createStructuredLog("OUTBOX_RETRY_START", Map.of(
                "context", contextName,
                "maxRetries", maxRetries
            )));

            eventPublisher.retryFailedEvents(maxRetries);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed outbox event retry", LoggingConfiguration.createStructuredLog("OUTBOX_RETRY_COMPLETED", Map.of(
                "context", contextName,
                "duration", duration
            )));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logError("outbox_retry", "RETRY_FAILED", e.getMessage(), e, Map.of(
                "context", contextName,
                "duration", duration
            ));

            logger.error("Error retrying failed outbox events", LoggingConfiguration.createStructuredLog("OUTBOX_RETRY_FAILED", Map.of(
                "context", contextName,
                "error", e.getMessage(),
                "duration", duration
            )), e);
        } finally {
            MDC.remove("module");
            MDC.remove("processor");
        }
    }

    /**
     * Log outbox statistics every 5 minutes asynchronously.
     * Can be overridden by subclasses to provide different scheduling.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    @Async
    public void logOutboxStats() {
        if (!isProcessingEnabled()) {
            return;
        }

        MDC.put("module", contextName);
        MDC.put("processor", "outbox");

        try {
            var stats = eventPublisher.getStats();
            logger.info("Outbox statistics", LoggingConfiguration.createStructuredLog("OUTBOX_STATS", Map.of(
                "context", contextName,
                "pending", stats.pending(),
                "processing", stats.processing(),
                "processed", stats.processed(),
                "failed", stats.failed(),
                "deadLetter", stats.deadLetter()
            )));
        } catch (Exception e) {
            LoggingConfiguration.logError("outbox_stats", "STATS_FAILED", e.getMessage(), e, Map.of(
                "context", contextName
            ));

            logger.error("Error logging outbox statistics", LoggingConfiguration.createStructuredLog("OUTBOX_STATS_FAILED", Map.of(
                "context", contextName,
                "error", e.getMessage()
            )), e);
        } finally {
            MDC.remove("module");
            MDC.remove("processor");
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
