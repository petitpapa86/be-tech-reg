package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.core.application.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatcher for integration events to registered handlers.
 * Automatically wraps handlers with idempotency support.
 */
@Component
public class IntegrationEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventDispatcher.class);

    private final Map<Class<? extends IntegrationEvent>, List<IIntegrationEventHandler<? extends IntegrationEvent>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final InboxMessageConsumerRepository consumerRepository;

    public IntegrationEventDispatcher(ApplicationContext applicationContext, InboxMessageConsumerRepository consumerRepository) {
        this.applicationContext = applicationContext;
        this.consumerRepository = consumerRepository;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register all IIntegrationEventHandler beans after the context is fully initialized
        @SuppressWarnings("unchecked")
        Map<String, IIntegrationEventHandler<? extends IntegrationEvent>> beans = (Map<String, IIntegrationEventHandler<? extends IntegrationEvent>>) (Map<String, ?>) applicationContext.getBeansOfType(IIntegrationEventHandler.class);
        if (beans != null && !beans.isEmpty()) {
            for (IIntegrationEventHandler<? extends IntegrationEvent> handler : beans.values()) {
                try {
                    registerHandler(handler);
                } catch (Exception e) {
                    logger.error("Failed to auto-register integration event handler {}", handler.getClass().getName(), e);
                }
            }
        } else {
            logger.debug("No IIntegrationEventHandler beans found during context refresh.");
        }
    }

    public void registerHandler(IIntegrationEventHandler<? extends IntegrationEvent> handler) {
        // Wrap with idempotency
        IdempotentIntegrationEventHandler<? extends IntegrationEvent> idempotentHandler =
            new IdempotentIntegrationEventHandler<>(handler, consumerRepository);

        Class<? extends IntegrationEvent> eventType = handler.getEventClass();
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(idempotentHandler);
        logger.info("Registered idempotent integration event handler: {} for event type: {}", handler.getHandlerName(), eventType.getName());
    }

    public void dispatch(IntegrationEvent event) {
        Class<? extends IntegrationEvent> eventType = event.getClass();
        List<IIntegrationEventHandler<? extends IntegrationEvent>> eventHandlers = handlers.get(eventType);

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            logger.warn("No handlers registered for integration event type: {}", eventType.getName());
            return;
        }

        for (IIntegrationEventHandler<? extends IntegrationEvent> handler : eventHandlers) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                IIntegrationEventHandler rawHandler = handler;
                rawHandler.handle(event);
                logger.debug("Dispatched integration event {} to handler {}", eventType.getName(), handler.getHandlerName());
            } catch (Exception e) {
                logger.error("Error dispatching integration event {} to handler {}: {}",
                    eventType.getName(), handler.getHandlerName(), e.getMessage(), e);
                // Continue with other handlers even if one fails
            }
        }
    }
}