package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.events.DomainEventHandler;
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
 * Registry for integration event handlers.
 * Provides access to registered handlers for event processing.
 */
@Component
public class IntegrationEventHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventHandlerRegistry.class);

    private final Map<Class<? extends DomainEvent>, List<DomainEventHandler<? extends DomainEvent>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public IntegrationEventHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register all DomainEventHandler beans after the context is fully initialized
        @SuppressWarnings("unchecked")
        Map<String, DomainEventHandler<? extends DomainEvent>> beans = (Map<String, DomainEventHandler<? extends DomainEvent>>) (Map<String, ?>) applicationContext.getBeansOfType(DomainEventHandler.class);
        if (beans != null && !beans.isEmpty()) {
            for (DomainEventHandler<? extends DomainEvent> handler : beans.values()) {
                try {
                    registerHandler(handler);
                } catch (Exception e) {
                    logger.error("Failed to auto-register domain event handler {}", handler.getClass().getName(), e);
                }
            }
        } else {
            logger.debug("No DomainEventHandler beans found during context refresh.");
        }
    }

    public void registerHandler(DomainEventHandler<? extends DomainEvent> handler) {
        Class<? extends DomainEvent> eventType = handler.eventClass();
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        logger.info("Registered integration event handler: {} for event type: {}", handler.getClass().getSimpleName(), eventType.getName());
    }

    public List<DomainEventHandler<? extends DomainEvent>> getHandlers(Class<? extends DomainEvent> eventType) {
        return handlers.getOrDefault(eventType, new ArrayList<>());
    }
}

