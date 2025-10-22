package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scheduled processor for outbox messages.
 * Deserializes and dispatches events to internal handlers.
 */
@Component
public class OutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int BATCH_SIZE = 10;

    private final Function<OutboxMessageStatus, List<OutboxMessageEntity>> findPendingFn;
    private final Consumer<String> markAsProcessedFn;
    private final BiConsumer<String, String> markAsFailedFn;
    private final Consumer<DomainEvent> dispatchFn;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(
            Function<OutboxMessageStatus, List<OutboxMessageEntity>> findPendingFn,
            Consumer<String> markAsProcessedFn,
            BiConsumer<String, String> markAsFailedFn,
            Consumer<DomainEvent> dispatchFn,
            ObjectMapper objectMapper) {
        this.findPendingFn = findPendingFn;
        this.markAsProcessedFn = markAsProcessedFn;
        this.markAsFailedFn = markAsFailedFn;
        this.dispatchFn = dispatchFn;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessageEntity> pendingMessages = findPendingFn.apply(OutboxMessageStatus.PENDING);

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
                markAsProcessedFn.accept(message.getId());
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process outbox message {}: {}", message.getId(), e.getMessage());
                markAsFailedFn.accept(message.getId(), e.getMessage());
            }
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