package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scheduled coordinator for inbox message processing.
 * Delegates fetching and per-message processing to dedicated components.
 */
@Component
public class ProcessInboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInboxJob.class);
    private static final int BATCH_SIZE = 10;

    private final Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> fetchPendingFn;
    private final MessageProcessor messageProcessor;
    private final Consumer<String> markAsProcessingFn;
    private final BiConsumer<String, String> markAsPermanentlyFailedFn;

    public ProcessInboxJob(Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> fetchPendingFn,
                          MessageProcessor messageProcessor,
                          Consumer<String> markAsProcessingFn,
                          BiConsumer<String, String> markAsPermanentlyFailedFn) {
        this.fetchPendingFn = fetchPendingFn;
        this.messageProcessor = messageProcessor;
        this.markAsProcessingFn = markAsProcessingFn;
        this.markAsPermanentlyFailedFn = markAsPermanentlyFailedFn;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void processInboxMessages() {
        List<InboxMessageEntity> pendingMessages = fetchPendingFn.apply(InboxMessageEntity.ProcessingStatus.PENDING);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} inbox messages", pendingMessages.size());

        int processedCount = 0;
        for (InboxMessageEntity message : pendingMessages) {
            if (processedCount >= BATCH_SIZE) {
                break;
            }

            // Mark message as processing to prevent concurrent processing
            try {
                markAsProcessingFn.accept(message.getId());
            } catch (Exception e) {
                logger.debug("Message {} is already being processed by another thread", message.getId());
                continue; // Skip this message as it's being processed by another thread
            }

            try {
                Result<Void> result = messageProcessor.process(message);
                if (result.isSuccess()) {
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Exception while processing inbox message {}: {}", message.getId(), e.getMessage());
                markAsPermanentlyFailedFn.accept(message.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} inbox messages successfully", processedCount);
    }
}