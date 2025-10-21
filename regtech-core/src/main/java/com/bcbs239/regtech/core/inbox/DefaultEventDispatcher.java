package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultEventDispatcher implements EventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultEventDispatcher.class);

    private final IntegrationEventHandlerRegistry registry;

    public DefaultEventDispatcher(IntegrationEventHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean dispatch(IntegrationEvent event, String messageId) {
        Class<? extends IntegrationEvent> eventType = event.getClass();
        List<DomainEventHandler<? extends DomainEvent>> handlers = registry.getHandlers(eventType);

        if (handlers == null || handlers.isEmpty()) {
            logger.warn("No handlers registered for integration event type: {}", eventType.getName());
            return true;
        }

        boolean allSuccess = true;
        for (DomainEventHandler<? extends DomainEvent> handler : handlers) {
            try {
                String handlerName = handler.getClass().getSimpleName();

                @SuppressWarnings({"rawtypes", "unchecked"})
                DomainEventHandler rawHandler = handler;
                boolean success = rawHandler.handle(event);

                if (success) {
                    logger.debug("Dispatched integration event {} to handler {}", eventType.getName(), handlerName);
                } else {
                    logger.error("Handler {} failed to process event {}", handlerName, eventType.getName());
                    allSuccess = false;
                }
            } catch (Exception e) {
                logger.error("Error dispatching integration event {} to handler {}: {}",
                        eventType.getName(), handler.getClass().getSimpleName(), e.getMessage(), e);
                allSuccess = false;
            }
        }

        return allSuccess;
    }
}

