package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.domain.DomainEvent;
import com.bcbs239.regtech.core.infrastructure.OutboxMessageEntity;
import com.bcbs239.regtech.core.infrastructure.OutboxMessageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final int BATCH_SIZE = 10;

    private final OutboxMessageRepository outboxMessageRepository;
    private final Consumer<DomainEvent> dispatchFn;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(
            OutboxMessageRepository outboxMessageRepository,
            Consumer<DomainEvent> dispatchFn,
            ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.dispatchFn = dispatchFn;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessageEntity> pendingMessages = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING);

        if (pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} outbox messages", pendingMessages.size());

        int processedCount = 0;
        for (OutboxMessageEntity message : pendingMessages) {
            if (processedCount >= BATCH_SIZE) {
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
        OutboxMessageEntity entity = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING)
            .stream()
            .filter(msg -> msg.getId().equals(messageId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        entity.setStatus(OutboxMessageStatus.PROCESSED);
        entity.setProcessedOnUtc(java.time.Instant.now());
        outboxMessageRepository.save(entity);
    }

    private void markAsFailed(String messageId, String errorMessage) {
        OutboxMessageEntity entity = outboxMessageRepository.findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING)
            .stream()
            .filter(msg -> msg.getId().equals(messageId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        entity.setStatus(OutboxMessageStatus.FAILED);
        entity.setErrorMessage(errorMessage);
        outboxMessageRepository.save(entity);
    }

    private void publishMessage(OutboxMessageEntity message) {
        try {
            String typeName = message.getType();
            Class<?> eventClass = Class.forName(typeName);

            if (!DomainEvent.class.isAssignableFrom(eventClass)) {
                throw new ClassNotFoundException("Event class does not implement DomainEvent: " + typeName);
            }

            @SuppressWarnings("unchecked")
            Class<? extends DomainEvent> domainEventClass = (Class<? extends DomainEvent>) eventClass;

            DomainEvent event = objectMapper.readValue(message.getContent(), domainEventClass);

            dispatchFn.accept(event);

            logger.info("Dispatched domain event from outbox: type={}, id={}", typeName, message.getId());

        } catch (ClassNotFoundException e) {
            logger.error("Domain event class not found for outbox message {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Domain event class not found", e);
        } catch (Exception e) {
            logger.error("Failed to dispatch message {}: {}", message.getId(), e.getMessage());
            throw new RuntimeException("Failed to dispatch message", e);
        }
    }
}
