package com.bcbs239.regtech.billing.infrastructure.outbox;

import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job to process outbox messages in the billing context.
 * Publishes stored events to the cross-module event bus for reliable delivery.
 */
@Component
public class ProcessOutboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessOutboxJob.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final CrossModuleEventBus eventBus;
    private final ObjectMapper objectMapper;

    public ProcessOutboxJob(
            OutboxMessageRepository outboxMessageRepository,
            CrossModuleEventBus eventBus,
            ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        logger.info("🚀 ProcessOutboxJob initialized for billing context");
    }

    /**
     * Process outbox messages every 30 seconds.
     * Publishes events to the cross-module event bus.
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    @Transactional
    public void processOutboxMessages() {
        logger.debug("🔄 Processing billing outbox messages...");

        List<OutboxMessage> messages = outboxMessageRepository.findMessagesForRetry();

        if (messages.isEmpty()) {
            logger.debug("📭 No outbox messages to process");
            return;
        }

        logger.info("📤 Processing {} outbox messages", messages.size());

        for (OutboxMessage message : messages) {
            try {
                processMessage(message);
            } catch (Exception e) {
                logger.error("Failed to process outbox message {}: {}", message.getId(), e.getMessage(), e);
                handleProcessingError(message, e);
            }
        }
    }

    /**
     * Process a single outbox message.
     */
    private void processMessage(OutboxMessage message) {
        logger.debug("📤 Publishing outbox message: type={}, correlationId={}",
            message.getEventType(), message.getCorrelationId());

        try {
            // Deserialize the event from JSON payload
            Class<?> eventClass = getEventClass(message.getEventType());
            Object event;
            try {
                event = objectMapper.readValue(message.getPayload(), eventClass);
            } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
                logger.error("Failed to parse payload for outbox message {}: {}", message.getId(), jpe.getMessage(), jpe);
                throw new RuntimeException("Failed to deserialize outbox payload", jpe);
            }

            // Publish to cross-module event bus
            eventBus.publishEvent(event);

            // Mark as processed
            message.markAsProcessed();
            outboxMessageRepository.save(message);

            logger.info("✅ Successfully published outbox message: type={}, correlationId={}, id={}",
                message.getEventType(), message.getCorrelationId(), message.getId());

        } catch (Exception e) {
            logger.error("Failed to deserialize or publish outbox message {}: {}", message.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Handle processing errors by incrementing retry count.
     */
    private void handleProcessingError(OutboxMessage message, Exception e) {
        message.incrementRetry(e.getMessage());
        outboxMessageRepository.save(message);

        if (!message.shouldRetry()) {
            logger.error("❌ Outbox message {} has exceeded max retries and will not be retried",
                message.getId());
        }
    }

    /**
     * Get the event class by event type name.
     * This is a simple mapping - in a real application, you might use a registry.
     */
    private Class<?> getEventClass(String eventType) {
        // This is a simplified mapping. In practice, you might have a more sophisticated
        // event type registry or use annotations to map event types to classes.
        try {
            switch (eventType) {
                case "PaymentProcessedEvent":
                    return Class.forName("com.bcbs239.regtech.billing.domain.events.PaymentProcessedEvent");
                case "BillingCycleStartedEvent":
                    return Class.forName("com.bcbs239.regtech.billing.domain.events.BillingCycleStartedEvent");
                // Add more event types as needed
                default:
                    throw new ClassNotFoundException("Unknown event type: " + eventType);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Failed to find event class for type: {}", eventType, e);
            throw new RuntimeException("Unknown event type: " + eventType, e);
        }
    }

    /**
     * Manual trigger for processing outbox messages (useful for testing).
     */
    public void processPendingEventsManually() {
        logger.info("🔄 Manually triggering outbox processing...");
        processOutboxMessages();
    }
}