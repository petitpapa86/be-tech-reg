package com.bcbs239.regtech.core.capabilities.inboxprocessing;

import com.bcbs239.regtech.core.capabilities.eventmanagement.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled coordinator for inbox message processing.
 * Delegates fetching and per-message processing to dedicated components.
 */
@Component
public class ProcessInboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInboxJob.class);
    private static final int BATCH_SIZE = 10;

    private final InboxMessageRepository inboxMessageRepository;
    private final TransactionTemplate transactionTemplate;
    private final IntegrationEventDeserializer deserializer;
    private final EventDispatcher dispatcher;

    public ProcessInboxJob(InboxMessageRepository inboxMessageRepository,
                           TransactionTemplate transactionTemplate,
                           IntegrationEventDeserializer deserializer,
                           EventDispatcher dispatcher) {
        this.inboxMessageRepository = inboxMessageRepository;
        this.transactionTemplate = transactionTemplate;
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processInboxMessages() {
        List<InboxMessageEntity> pendingMessages = inboxMessageRepository.findByProcessingStatusOrderByReceivedAt(InboxMessageEntity.ProcessingStatus.PENDING);

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
                InboxFunctions.markAsProcessing(inboxMessageRepository.getEntityManager(), transactionTemplate, new InboxFunctions.MarkAsProcessingRequest(message.getId()));
            } catch (Exception e) {
                logger.debug("Message {} is already being processed by another thread", message.getId());
                continue; // Skip this message as it's being processed by another thread
            }


            Result<IntegrationEvent> deserializeResult = deserializer.deserialize(message.getEventType(), message.getEventData());
            if (deserializeResult.isFailure()) {
                String errorMessage = deserializeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown deserialization error");
                logger.error("Deserialization failed for message {}: {}", message.getId(), errorMessage);
                InboxFunctions.markAsPermanentlyFailed(inboxMessageRepository.getEntityManager(), transactionTemplate, new InboxFunctions.MarkAsPermanentlyFailedRequest(message.getId(), "Deserialization failed: " + errorMessage));
                continue;
            }


            var event = deserializeResult.getValue().get();
            boolean success = dispatcher.dispatch(event, message.getId());
            if (success) {
                InboxFunctions.markAsProcessed(inboxMessageRepository.getEntityManager(), transactionTemplate, new InboxFunctions.MarkAsProcessedRequest(message.getId(), Instant.now()));
                processedCount.getAndIncrement();
            } else {
                InboxFunctions.markAsPermanentlyFailed(inboxMessageRepository.getEntityManager(), transactionTemplate, new InboxFunctions.MarkAsPermanentlyFailedRequest(message.getId(), "One or more handlers failed"));
                logger.warn("One or more handlers failed for message {}", message.getId());
            }

        }

        logger.info("Processed {} inbox messages successfully", processedCount);
    }
}

