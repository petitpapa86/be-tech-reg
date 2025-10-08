package com.bcbs239.regtech.billing.infrastructure.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled processor for outbox events to ensure reliable event delivery.
 * Processes pending and failed events at regular intervals using closure-based event publisher.
 */
@Component
@ConditionalOnProperty(name = "billing.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final int MAX_RETRIES = 3;

    private final BillingEventPublisher eventPublisher;

    public OutboxEventProcessor(BillingEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Process pending events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processPendingEvents() {
        try {
            logger.debug("Processing pending outbox events");
            eventPublisher.processPendingEvents();
        } catch (Exception e) {
            logger.error("Error processing pending outbox events", e);
        }
    }

    /**
     * Retry failed events every 2 minutes.
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 60000)
    public void retryFailedEvents() {
        try {
            logger.debug("Retrying failed outbox events");
            eventPublisher.retryFailedEvents(MAX_RETRIES);
        } catch (Exception e) {
            logger.error("Error retrying failed outbox events", e);
        }
    }

    /**
     * Log outbox statistics every 5 minutes for monitoring.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void logOutboxStats() {
        try {
            var stats = eventPublisher.getStats();
            logger.info("Outbox stats - Pending: {}, Processing: {}, Processed: {}, Failed: {}, Dead Letter: {}",
                stats.pending(), stats.processing(), stats.processed(), stats.failed(), stats.deadLetter());
        } catch (Exception e) {
            logger.error("Error logging outbox statistics", e);
        }
    }
}