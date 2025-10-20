package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.core.application.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decorator that ensures integration event handlers are idempotent.
 * Tracks which handlers have processed which events to prevent duplicate processing.
 */
public class IdempotentIntegrationEventHandler<T extends IntegrationEvent> implements IIntegrationEventHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(IdempotentIntegrationEventHandler.class);

    private final IIntegrationEventHandler<T> decoratedHandler;
    private final InboxMessageConsumerRepository consumerRepository;

    public IdempotentIntegrationEventHandler(
            IIntegrationEventHandler<T> decoratedHandler,
            InboxMessageConsumerRepository consumerRepository) {
        this.decoratedHandler = decoratedHandler;
        this.consumerRepository = consumerRepository;
    }

    @Override
    @Transactional
    public void handle(T event) {
        String handlerName = decoratedHandler.getHandlerName();
        String messageId = event.getId().toString();

        // Check if this handler has already processed this event
        if (consumerRepository.existsByInboxMessageIdAndName(messageId, handlerName)) {
            logger.debug("Event {} already processed by handler {}, skipping", messageId, handlerName);
            return;
        }

        try {
            // Process the event - let exceptions bubble up to inbox processor
            decoratedHandler.handle(event);

            // Mark as processed only if successful
            consumerRepository.save(new InboxMessageConsumer(messageId, handlerName));
            logger.debug("Event {} successfully processed by handler {}", messageId, handlerName);

        } catch (Exception e) {
            logger.error("Failed to process event {} with handler {}: {}", messageId, handlerName, e.getMessage(), e);
            // Re-throw to let inbox processor handle the failure (retry/dead letter)
            throw e;
        }
    }

    @Override
    public Class<T> getEventClass() {
        return decoratedHandler.getEventClass();
    }

    @Override
    public String getHandlerName() {
        return decoratedHandler.getHandlerName();
    }
}