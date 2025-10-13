package com.bcbs239.regtech.billing.infrastructure.messaging;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inbox processor job that reads events from the inbox and processes them asynchronously.
 * This implements the inbox pattern for reliable event processing.
 */
@Component("billingInboxProcessorJob")
public class InboxProcessorJob {

    private static final Logger logger = LoggerFactory.getLogger(InboxProcessorJob.class);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;

    private final ObjectMapper objectMapper;
    private final java.util.Map<String, java.util.function.Function<Object, Boolean>> handlers;
    private final java.util.function.Function<Integer, java.util.List<InboxEventEntity>> inboxLoader;
    private final java.util.function.Function<String, Boolean> markProcessed;
    private final java.util.function.BiFunction<String, String, Boolean> markFailed;

    public InboxProcessorJob(ObjectMapper objectMapper,
                            @Qualifier("billingInboxHandlers") java.util.Map<String, java.util.function.Function<Object, Boolean>> handlers,
                            java.util.function.Function<Integer, java.util.List<InboxEventEntity>> inboxLoader,
                            java.util.function.Function<String, Boolean> markProcessed,
                            java.util.function.BiFunction<String, String, Boolean> markFailed) {
        this.objectMapper = objectMapper;
        this.handlers = handlers;
        this.inboxLoader = inboxLoader;
        this.markProcessed = markProcessed;
        this.markFailed = markFailed;
    }

    /**
     * Scheduled job to process inbox events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    @Transactional
    public void processInboxEvents() {
        logger.debug("Starting inbox event processing");

        List<InboxEventEntity> pendingEvents = inboxLoader.apply(BATCH_SIZE);

        for (InboxEventEntity inboxEvent : pendingEvents) {
            processInboxEvent(inboxEvent);
        }

        logger.debug("Completed inbox event processing: {} events processed", pendingEvents.size());
    }

    /**
     * Process a single inbox event.
     */
    @Transactional
    public void processInboxEvent(InboxEventEntity inboxEvent) {
        try {
            // Mark as processing (optimistic - persisted by closure implementations if needed)
            inboxEvent.markAsProcessing();

            // Deserialize and process the event
            boolean success = processEventByType(inboxEvent);

            if (success) {
                // mark processed via closure
                boolean ok = markProcessed.apply(inboxEvent.getId());
                if (ok) {
                    logger.info("Successfully processed inbox event: id={}, type={}", inboxEvent.getId(), inboxEvent.getEventType());
                } else {
                    logger.warn("Failed to mark inbox event {} as processed via closure", inboxEvent.getId());
                }
            } else {
                handleProcessingFailure(inboxEvent, "Processing failed");
            }

        } catch (Exception e) {
            logger.error("Error processing inbox event {}: {}", inboxEvent.getId(), e.getMessage(), e);
            handleProcessingFailure(inboxEvent, e.getMessage());
        }
    }

    /**
     * Process event based on its type.
     */
    private boolean processEventByType(InboxEventEntity inboxEvent) {
        try {
            java.util.function.Function<Object, Boolean> handler = handlers.get(inboxEvent.getEventType());
            if (handler == null) {
                logger.warn("Unknown event type in inbox: {}", inboxEvent.getEventType());
                return false;
            }

            // Deserialize the stored JSON data to a generic Object (usually Map) then pass to handler
            Object raw = objectMapper.readValue(inboxEvent.getEventData(), Object.class);
            return handler.apply(raw);
        } catch (Exception e) {
            logger.error("Failed to deserialize/process event {}: {}", inboxEvent.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle processing failure with retry logic.
     */
    private void handleProcessingFailure(InboxEventEntity inboxEvent, String error) {
        inboxEvent.setLastError(error);

        if (inboxEvent.canRetry(MAX_RETRIES)) {
            inboxEvent.setProcessingStatus(InboxEventEntity.ProcessingStatus.PENDING);
            logger.warn("Inbox event {} failed, will retry (attempt {}/{}): {}",
                inboxEvent.getId(), inboxEvent.getRetryCount(), MAX_RETRIES, error);
        } else {
            inboxEvent.markAsDeadLetter();
            logger.error("Inbox event {} failed permanently after {} retries: {}",
                inboxEvent.getId(), MAX_RETRIES, error);
        }
    }

    // Using closure-based loader `inboxLoader` instead of direct EntityManager queries

    /**
     * Manually trigger inbox processing (for testing or admin purposes).
     */
    public void runOnce() {
        logger.info("Manually triggering inbox processing");
        processInboxEvents();
    }
}