package com.bcbs239.regtech.core.application.integration;

import com.bcbs239.regtech.core.domain.IIntegrationEventHandler;
import com.bcbs239.regtech.core.domain.IntegrationEvent;
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
        List<IIntegrationEventHandler<? extends IntegrationEvent>> handlers = registry.getHandlers(eventType);

        if (handlers == null || handlers.isEmpty()) {
            logger.warn("No handlers registered for integration event type: {}", eventType.getName());
            return true;
        }

        boolean allSuccess = true;
        for (IIntegrationEventHandler<? extends IntegrationEvent> handler : handlers) {

            String handlerName = handler.getClass().getSimpleName();

            try {
                @SuppressWarnings({"rawtypes"})
                IIntegrationEventHandler rawHandler = handler;
                rawHandler.handle(event);
                logger.debug("Dispatched integration event {} to handler {}", eventType.getName(), handlerName);
            } catch (Exception e) {
                logger.error("Handler {} failed to process event {}", handlerName, eventType.getName(), e);
                allSuccess = false;
            }

        }

        return allSuccess;
    }
}

