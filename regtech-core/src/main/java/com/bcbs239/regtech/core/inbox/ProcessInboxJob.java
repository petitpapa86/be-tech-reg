package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled processor for inbox messages.
 * Deserializes and dispatches events to integration event handlers.
 */
@Component
public class ProcessInboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInboxJob.class);
    private static final int BATCH_SIZE = 10;

    private final InboxMessageJpaRepository inboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final IntegrationEventDispatcher integrationEventDispatcher;

    public ProcessInboxJob(InboxMessageJpaRepository inboxMessageRepository,
                          ObjectMapper objectMapper,
                          IntegrationEventDispatcher integrationEventDispatcher) {
        this.inboxMessageRepository = inboxMessageRepository;
        this.objectMapper = objectMapper;
        this.integrationEventDispatcher = integrationEventDispatcher;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processInboxMessages() {
        List<InboxMessageEntity> pendingMessages = inboxMessageRepository
            .findPendingMessages(InboxMessageEntity.ProcessingStatus.PENDING);

        if (pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} inbox messages", pendingMessages.size());

        int processedCount = 0;
        for (InboxMessageEntity message : pendingMessages) {
            if (processedCount >= BATCH_SIZE) {
                break;
            }

            try {
                processMessage(message);
                inboxMessageRepository.markAsProcessed(message.getId(), java.time.Instant.now());
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process inbox message {}: {}", message.getId(), e.getMessage());
                inboxMessageRepository.markAsPermanentlyFailed(message.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} inbox messages successfully", processedCount);
    }

    private void processMessage(InboxMessageEntity message) {
        try {
            String typeName = message.getEventType();
            Class<?> eventClass = Class.forName(typeName);

            if (!IntegrationEvent.class.isAssignableFrom(eventClass)) {
                throw new ClassNotFoundException("Event class does not implement IntegrationEvent: " + typeName);
            }

            @SuppressWarnings("unchecked")
            Class<? extends IntegrationEvent> integrationEventClass = (Class<? extends IntegrationEvent>) eventClass;

            IntegrationEvent event = objectMapper.readValue(message.getEventData(), integrationEventClass);

            integrationEventDispatcher.dispatch(event);

            logger.info("Dispatched integration event from inbox: type={}, id={}", typeName, message.getId());

        } catch (ClassNotFoundException e) {
            logger.error("Integration event class not found for inbox message {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Integration event class not found", e);
        } catch (Exception e) {
            logger.error("Failed to process message {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Failed to process message", e);
        }
    }
}