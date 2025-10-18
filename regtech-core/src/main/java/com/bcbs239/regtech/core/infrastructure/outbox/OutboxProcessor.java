package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
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
    private final ObjectMapper objectMapper;
    private final DomainEventDispatcher domainEventDispatcher;

    public OutboxProcessor(OutboxMessageRepository outboxMessageRepository,
                          ObjectMapper objectMapper,
                          DomainEventDispatcher domainEventDispatcher) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessageEntity> pendingMessages = outboxMessageRepository
            .findByStatusOrderByOccurredOnUtc(OutboxMessageStatus.PENDING);

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
                message.markAsProcessed();
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process outbox message {}: {}", message.getId(), e.getMessage());
                message.markAsFailed(e.getMessage());
            }
        }

        if (processedCount > 0) {
            outboxMessageRepository.saveAll(pendingMessages.subList(0, processedCount));
        }
        logger.info("Processed {} outbox messages successfully", processedCount);
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

            domainEventDispatcher.dispatch(event);

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