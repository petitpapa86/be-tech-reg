package com.bcbs239.regtech.billing.infrastructure.messaging;

import com.bcbs239.regtech.billing.application.events.UserRegisteredEventHandler;
import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inbox processor job that reads events from the inbox and processes them asynchronously.
 * This implements the inbox pattern for reliable event processing.
 */
@Component
public class InboxProcessorJob {

    private static final Logger logger = LoggerFactory.getLogger(InboxProcessorJob.class);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;
    private final UserRegisteredEventHandler userRegisteredEventHandler;

    public InboxProcessorJob(ObjectMapper objectMapper,
                           UserRegisteredEventHandler userRegisteredEventHandler) {
        this.objectMapper = objectMapper;
        this.userRegisteredEventHandler = userRegisteredEventHandler;
    }

    /**
     * Scheduled job to process inbox events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    @Transactional
    public void processInboxEvents() {
        logger.debug("Starting inbox event processing");

        List<InboxEventEntity> pendingEvents = findPendingEvents();

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
            // Mark as processing
            inboxEvent.markAsProcessing();
            entityManager.merge(inboxEvent);

            // Deserialize and process the event
            boolean success = processEventByType(inboxEvent);

            if (success) {
                inboxEvent.markAsProcessed();
                logger.info("Successfully processed inbox event: id={}, type={}",
                    inboxEvent.getId(), inboxEvent.getEventType());
            } else {
                handleProcessingFailure(inboxEvent, "Processing failed");
            }

            entityManager.merge(inboxEvent);

        } catch (Exception e) {
            logger.error("Error processing inbox event {}: {}", inboxEvent.getId(), e.getMessage(), e);
            handleProcessingFailure(inboxEvent, e.getMessage());
            entityManager.merge(inboxEvent);
        }
    }

    /**
     * Process event based on its type.
     */
    private boolean processEventByType(InboxEventEntity inboxEvent) {
        try {
            switch (inboxEvent.getEventType()) {
                case "UserRegisteredIntegrationEvent":
                    UserRegisteredIntegrationEvent event = objectMapper.readValue(
                        inboxEvent.getEventData(), UserRegisteredIntegrationEvent.class);
                    userRegisteredEventHandler.handle(event);
                    return true;
                default:
                    logger.warn("Unknown event type in inbox: {}", inboxEvent.getEventType());
                    return false;
            }
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

    /**
     * Find pending inbox events to process.
     */
    private List<InboxEventEntity> findPendingEvents() {
        Query query = entityManager.createQuery(
            "SELECT e FROM InboxEventEntity e WHERE e.processingStatus = :status ORDER BY e.receivedAt ASC",
            InboxEventEntity.class
        );
        query.setParameter("status", InboxEventEntity.ProcessingStatus.PENDING);
        query.setMaxResults(BATCH_SIZE);

        @SuppressWarnings("unchecked")
        List<InboxEventEntity> results = query.getResultList();
        return results;
    }

    /**
     * Manually trigger inbox processing (for testing or admin purposes).
     */
    public void runOnce() {
        logger.info("Manually triggering inbox processing");
        processInboxEvents();
    }
}