package com.bcbs239.regtech.core.capabilities.outboxhandling;

import com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEvent;
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
import java.util.function.Consumer;

/**
 * Dispatcher for domain events to registered handlers.
 */
@Component
public class DomainEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventDispatcher.class);

    private final Map<Class<? extends DomainEvent>, List<com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public DomainEventDispatcher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register all DomainEventHandler beans after the context is fully initialized
        @SuppressWarnings("unchecked")
        Map<String, com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent>> beans = (Map<String, com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent>>) (Map<String, ?>) applicationContext.getBeansOfType(com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler.class);
        if (beans != null && !beans.isEmpty()) {
            for (com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent> handler : beans.values()) {
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

    public void registerHandler(com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent> handler) {
        Class<? extends DomainEvent> eventType = handler.eventClass();
        handlers.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
        logger.info("Registered event handler {} for event type {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
    }

    public void dispatch(DomainEvent event) {
        // Support subclasses and proxies by matching registered handler types that are assignable from the event's class
        List<com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent>> matchedHandlers = new ArrayList<>();

        for (Map.Entry<Class<? extends DomainEvent>, List<com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent>>> entry : handlers.entrySet()) {
            Class<? extends DomainEvent> registeredType = entry.getKey();
            if (registeredType.isAssignableFrom(event.getClass())) {
                matchedHandlers.addAll(entry.getValue());
            }
        }

        if (!matchedHandlers.isEmpty()) {
            for (com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<? extends DomainEvent> handler : matchedHandlers) {
                try {
                    @SuppressWarnings("unchecked")
                    com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<DomainEvent> h = (com.bcbs239.regtech.core.capabilities.eventmanagement.DomainEventHandler<DomainEvent>) handler;
                    h.handle(event);
                    logger.info("Dispatched event {} to handler {}", event.getClass().getSimpleName(), handler.getClass().getSimpleName());
                } catch (Exception e) {
                    logger.error("Failed to handle event {} with handler {}", event.getClass().getSimpleName(), handler.getClass().getSimpleName(), e);
                }
            }
        } else {
            logger.debug("No handlers found for event type {}", event.getClass().getSimpleName());
        }
    }

    public Consumer<DomainEvent> dispatchFn() {
        return this::dispatch;
    }
}
