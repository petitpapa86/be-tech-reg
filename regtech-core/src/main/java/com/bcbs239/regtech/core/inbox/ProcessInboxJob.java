package com.bcbs239.regtech.core.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled coordinator for inbox message processing.
 * Delegates fetching and per-message processing to dedicated components.
 */
@Component
public class ProcessInboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInboxJob.class);
    private static final int BATCH_SIZE = 10;

    private final InboxMessageOperations inboxOperations;
    private final MessageProcessor messageProcessor;

    public ProcessInboxJob(InboxMessageOperations inboxOperations,
                          MessageProcessor messageProcessor) {
        this.inboxOperations = inboxOperations;
        this.messageProcessor = messageProcessor;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void processInboxMessages() {
        List<InboxMessageEntity> pendingMessages = inboxOperations.findPendingMessagesFn().apply(InboxMessageEntity.ProcessingStatus.PENDING);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} inbox messages", pendingMessages.size());

        int processedCount = 0;
        for (InboxMessageEntity message : pendingMessages) {
            if (processedCount >= BATCH_SIZE) {
                break;
            }

            try {
                messageProcessor.process(message);
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process inbox message {}: {}", message.getId(), e.getMessage());
                // The MessageProcessor is responsible for marking failures; continue with next message
            }
        }

        logger.info("Processed {} inbox messages successfully", processedCount);
    }
}