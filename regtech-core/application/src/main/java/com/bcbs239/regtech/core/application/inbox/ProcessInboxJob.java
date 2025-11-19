package com.bcbs239.regtech.core.application.inbox;

import com.bcbs239.regtech.core.application.eventprocessing.CoreIntegrationEventDeserializer;
import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.bcbs239.regtech.core.domain.inbox.InboxMessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled coordinator for inbox message processing.
 * Delegates fetching and per-message processing to dedicated components.
 */
public class ProcessInboxJob {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInboxJob.class);

    private final IInboxMessageRepository inboxMessageRepository;
    private final DomainEventBus dispatcher;
    private final InboxOptions inboxOptions;
    private final CoreIntegrationEventDeserializer deserializer;

    public ProcessInboxJob(IInboxMessageRepository inboxMessageRepository, DomainEventBus dispatcher, InboxOptions inboxOptions, CoreIntegrationEventDeserializer deserializer) {
        this.inboxMessageRepository = inboxMessageRepository;
        this.dispatcher = dispatcher;
        this.inboxOptions = inboxOptions;
        this.deserializer = deserializer;
    }


    // Use the configured Duration from InboxOptions (bean) and convert to millis via SpEL
    @Scheduled(fixedDelayString = "#{@inboxOptions.getPollInterval().toMillis()}")
    @Transactional
    public void processInboxMessages() {
        List<InboxMessage> pendingMessages = inboxMessageRepository.findByProcessingStatusOrderByReceivedAt(InboxMessageStatus.PENDING);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        int batchSize = Math.max(1, inboxOptions.getBatchSize());
        List<InboxMessage> toProcess = pendingMessages.stream().limit(batchSize).collect(Collectors.toList());

        logger.info("Processing {} inbox messages (batch size {})", toProcess.size(), batchSize);

        for (InboxMessage message : toProcess) {

            try {
                inboxMessageRepository.markAsProcessing(message.getId());
            } catch (Exception e) {
                logger.debug("Message {} is already being processed by another thread", message.getId());
                continue; // Skip this message as it's being processed by another thread
            }


            Result<DomainEvent> deserializeResult = deserializer.deserialize(message.getEventType(), message.getContent());
            if (deserializeResult.isFailure()) {
                String errorMessage = deserializeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown deserialization error");
                logger.error("Deserialization failed for message {}: {}", message.getId(), errorMessage);
                inboxMessageRepository.markAsPermanentlyFailed(message.getId());
                continue;
            }


            DomainEvent event = deserializeResult.getValue().orElseThrow();
            try{
                logger.info("Publishing replayed integration event: {} (eventId={})", event.getClass().getSimpleName(), event.getEventId());
                dispatcher.publishFromInbox(event);
                inboxMessageRepository.markAsProcessed(message.getId(), Instant.now());
                logger.info("Successfully processed inbox message {} and published event {}", message.getId(), event.getClass().getSimpleName());
            } catch (Exception e){
                inboxMessageRepository.markAsPermanentlyFailed(message.getId());
                logger.error("Processing failed for inbox message {}: {}", message.getId(), e.getMessage(), e);
            }

        }
    }
}

