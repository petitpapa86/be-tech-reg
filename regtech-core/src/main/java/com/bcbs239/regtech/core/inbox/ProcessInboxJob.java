package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final IntegrationEventDeserializer deserializer;
    private final EventDispatcher dispatcher;
    private final Function<InboxFunctions.MarkAsProcessingRequest, Integer> markAsProcessingCoreFn;
    private final Function<InboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedCoreFn;
    private final Function<InboxFunctions.MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedCoreFn;

    public ProcessInboxJob(Function<InboxMessageEntity.ProcessingStatus, List<InboxMessageEntity>> fetchPendingFn,
                           IntegrationEventDeserializer deserializer,
                           EventDispatcher dispatcher,
                           Function<InboxFunctions.MarkAsProcessingRequest, Integer> markAsProcessingCoreFn,
                           Function<InboxFunctions.MarkAsProcessedRequest, Integer> markAsProcessedCoreFn,
                           Function<InboxFunctions.MarkAsPermanentlyFailedRequest, Integer> markAsPermanentlyFailedCoreFn) {
        this.fetchPendingFn = fetchPendingFn;
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
        this.markAsProcessingCoreFn = markAsProcessingCoreFn;
        this.markAsProcessedCoreFn = markAsProcessedCoreFn;
        this.markAsPermanentlyFailedCoreFn = markAsPermanentlyFailedCoreFn;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void processInboxMessages() {
        List<InboxMessageEntity> pendingMessages = fetchPendingFn.apply(InboxMessageEntity.ProcessingStatus.PENDING);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} inbox messages", pendingMessages.size());

        AtomicInteger processedCount = new AtomicInteger();
        for (InboxMessageEntity message : pendingMessages) {
            if (processedCount.get() >= BATCH_SIZE) {
                break;
            }


            // Mark message as processing to prevent concurrent processing
            try {
                markAsProcessingCoreFn.apply(new InboxFunctions.MarkAsProcessingRequest(message.getId()));
            } catch (Exception e) {
                logger.debug("Message {} is already being processed by another thread", message.getId());
                continue; // Skip this message as it's being processed by another thread
            }


            Result<IntegrationEvent> deserializeResult = deserializer.deserialize(message.getEventType(), message.getEventData());
            if (deserializeResult.isFailure()) {
                String errorMessage = deserializeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown deserialization error");
                logger.error("Deserialization failed for message {}: {}", message.getId(), errorMessage);
                markAsPermanentlyFailedCoreFn.apply(new InboxFunctions.MarkAsPermanentlyFailedRequest(message.getId(), "Deserialization failed: " + errorMessage));
                continue;
            }


            var event = deserializeResult.getValue().get();
            boolean success = dispatcher.dispatch(event, message.getId());
            if (success) {
                markAsProcessedCoreFn.apply(new InboxFunctions.MarkAsProcessedRequest(message.getId(), Instant.now()));
                processedCount.getAndIncrement();
            } else {
                markAsPermanentlyFailedCoreFn.apply(new InboxFunctions.MarkAsPermanentlyFailedRequest(message.getId(), "One or more handlers failed"));
                logger.warn("One or more handlers failed for message {}", message.getId());
            }

        }

        logger.info("Processed {} inbox messages successfully", processedCount);
    }
}