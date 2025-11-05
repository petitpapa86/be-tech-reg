package com.bcbs239.regtech.core.application.integration;

import com.bcbs239.regtech.core.domain.IIntegrationEventHandler;
import com.bcbs239.regtech.core.domain.IntegrationEvent;
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

    private final Map<Class<? extends IntegrationEvent>, List<IIntegrationEventHandler<? extends IntegrationEvent>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public IntegrationEventHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register all IIntegrationEventHandler beans after the context is fully initialized
        @SuppressWarnings("unchecked")
        Map<String, IIntegrationEventHandler<? extends IntegrationEvent>> beans = (Map<String, IIntegrationEventHandler<? extends IntegrationEvent>>) (Map<String, ?>) applicationContext.getBeansOfType(IIntegrationEventHandler.class);
        if (!beans.isEmpty()) {
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
        Class<? extends IntegrationEvent> eventType = handler.getEventClass();
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        logger.info("Registered integration event handler: {} for event type: {}", handler.getHandlerName(), eventType.getName());
    }

    public List<IIntegrationEventHandler<? extends IntegrationEvent>> getHandlers(Class<? extends IntegrationEvent> eventType) {
        return handlers.getOrDefault(eventType, new ArrayList<>());
    }
}

