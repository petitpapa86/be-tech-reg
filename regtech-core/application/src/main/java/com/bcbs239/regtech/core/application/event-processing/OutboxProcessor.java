package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.OutboxMessageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

/**
 * Scheduled processor for outbox messages.
 * Deserializes and dispatches events to internal handlers.
 */
@Component
public class OutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int BATCH_SIZE = 10;

    private final IOutboxMessageRepository outboxMessageRepository;
    private final Consumer<DomainEvent> dispatchFn;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(
            IOutboxMessageRepository outboxMessageRepository,
            Consumer<DomainEvent> dispatchFn,
            ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.dispatchFn = dispatchFn;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findPendingMessages();

        if (pendingMessages.isEmpty()) {
            return;
        }

        logger.info("Processing {} outbox messages", pendingMessages.size());

        int processedCount = 0;
        for (OutboxMessage message : pendingMessages) {
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
        OutboxMessage message = outboxMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // Since the domain interface doesn't have setters, we need to work with the infrastructure entity
        if (message instanceof com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity) {
            com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity entity =
                (com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity) message;
            entity.setStatus(OutboxMessageStatus.PROCESSED);
            entity.setProcessedOnUtc(java.time.Instant.now());
            outboxMessageRepository.save(entity);
        }
    }

    private void markAsFailed(String messageId, String errorMessage) {
        OutboxMessage message = outboxMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // Since the domain interface doesn't have setters, we need to work with the infrastructure entity
        if (message instanceof com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity) {
            com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity entity =
                (com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity) message;
            entity.setStatus(OutboxMessageStatus.FAILED);
            entity.setErrorMessage(errorMessage);
            outboxMessageRepository.save(entity);
        }
    }

    private void publishMessage(OutboxMessage message) {
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
