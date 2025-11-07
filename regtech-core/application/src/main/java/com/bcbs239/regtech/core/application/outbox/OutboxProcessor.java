package com.bcbs239.regtech.core.application.outbox;


import com.bcbs239.regtech.core.application.integration.DomainEventDispatcher;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
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
    private final DomainEventDispatcher domainEventDispatcher;
    private final ObjectMapper objectMapper;
    private final OutboxOptions outboxOptions;

    public OutboxProcessor(
            IOutboxMessageRepository outboxMessageRepository, DomainEventDispatcher domainEventDispatcher,
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
                publishMessage(message);
                markAsProcessed(message.getId());
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process outbox message {}: {}", message.getId(), e.getMessage());
                markAsFailed(message.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} outbox messages successfully", processedCount);
    }

    private void markAsProcessed(String messageId) {
        OutboxMessage outboxMessage = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING)
                .stream()
                .filter(msg -> msg.getId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        outboxMessage.setStatus(OutboxMessageStatus.PROCESSED);
        outboxMessage.setProcessedOnUtc(java.time.Instant.now());
        outboxMessageRepository.save(outboxMessage);
    }

    private void markAsFailed(String messageId, String errorMessage) {
        OutboxMessage outboxMessage = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING)
                .stream()
                .filter(msg -> msg.getId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        outboxMessage.setStatus(OutboxMessageStatus.FAILED);
        outboxMessage.setErrorMessage(errorMessage);
        outboxMessageRepository.save(outboxMessage);
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

        domainEventDispatcher.dispatch(event);

        logger.info("Dispatched domain event from outbox: type={}, id={}", typeName, message.getId());

    }

    @Scheduled(fixedDelayString = "#{@outboxOptions.getPollInterval().toMillis()}", initialDelayString = "#{@outboxOptions.getPollInterval().toMillis()}")
    @Async
    @Transactional
    public void retryFailedEvents() {

    }
}

