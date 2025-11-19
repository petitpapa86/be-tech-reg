package com.bcbs239.regtech.core.application.outbox;


import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import com.bcbs239.regtech.core.domain.outbox.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessage;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessageStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Scheduled processor for outbox messages.
 * Deserializes and dispatches events to internal handlers.
 */
@Component
public class OutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);

    private final IOutboxMessageRepository outboxMessageRepository;
    private final DomainEventBus domainEventDispatcher;
    private final ObjectMapper objectMapper;
    private final OutboxOptions outboxOptions;

    public OutboxProcessor(
            IOutboxMessageRepository outboxMessageRepository, DomainEventBus domainEventDispatcher,
            ObjectMapper objectMapper, OutboxOptions outboxOptions) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.domainEventDispatcher = domainEventDispatcher;
        this.objectMapper = objectMapper;
        this.outboxOptions = outboxOptions;
    }

    @Scheduled(fixedDelayString = "#{@outboxOptions.getPollInterval().toMillis()}") // Run according to configured poll interval
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING);

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        int batchSize = Math.max(1, outboxOptions.getBatchSize());

        logger.info("Processing {} outbox messages (batch size {})", pendingMessages.size(), batchSize);

        int processedCount = 0;
        for (OutboxMessage message : pendingMessages) {
            if (processedCount >= batchSize) {
                break;
            }

            try {
                // Attempt to atomically claim the message (set to PROCESSING). If another worker claimed it, skip.
                int claimed = outboxMessageRepository.markAsProcessing(message.getId());
                if (claimed == 0) {
                    logger.debug("Outbox message {} already claimed by another worker, skipping", message.getId());
                    continue;
                }

                publishMessage(message);

                // Mark processed after successful publish
                outboxMessageRepository.markAsProcessed(message.getId(), java.time.Instant.now());
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process outbox message {}: {}", message.getId(), e.getMessage());
                markAsFailed(message.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} outbox messages successfully", processedCount);
    }

    private void markAsFailed(String messageId, String errorMessage) {
        try {
            outboxMessageRepository.markAsFailed(messageId, errorMessage);
        } catch (Exception ex) {
            logger.error("Failed to mark outbox message {} as failed: {}", messageId, ex.getMessage(), ex);
        }
    }

    private void publishMessage(OutboxMessage message) throws ClassNotFoundException, JsonProcessingException {

        String typeName = message.getType();
        Class<?> eventClass = Class.forName(typeName);

        if (!DomainEvent.class.isAssignableFrom(eventClass)) {
            throw new ClassNotFoundException("Event class does not implement DomainEvent: " + typeName);
        }

        @SuppressWarnings("unchecked")
        Class<? extends DomainEvent> domainEventClass = (Class<? extends DomainEvent>) eventClass;

        DomainEvent event = objectMapper.readValue(message.getContent(), domainEventClass);

        // Publish all deserialized domain events as outbox replays so listeners can avoid
        // emitting side-effects again (for example, re-publishing integration events).
        domainEventDispatcher.publishAsReplay(event);
        logger.info("Dispatched domain event (outbox replay) from outbox: type={}, id={}", typeName, message.getId());

    }

    @Scheduled(fixedDelayString = "#{@outboxOptions.getPollInterval().toMillis()}", initialDelayString = "#{@outboxOptions.getPollInterval().toMillis()}")
    @Async
    @Transactional
    public void retryFailedEvents() {

    }
}

