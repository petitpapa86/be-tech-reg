package com.bcbs239.regtech.core.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEventHandler;
import com.bcbs239.regtech.core.domain.events.BaseEvent;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
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
 * Dispatcher for domain events to registered handlers.
 */
@Component
public class DomainEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventDispatcher.class);

    private final Map<Class<? extends BaseEvent>, List<DomainEventHandler<? extends BaseEvent>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public DomainEventDispatcher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register all DomainEventHandler beans after the context is fully initialized

        var beans =  applicationContext.getBeansOfType(DomainEventHandler.class);
        if (!beans.isEmpty()) {
            for (var handler : beans.values()) {
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

    public void registerHandler(DomainEventHandler<? extends BaseEvent> handler) {
        Class<? extends BaseEvent> eventType = handler.eventClass();
        handlers.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
        logger.info("Registered event handler {} for event type {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
    }

    public boolean dispatch(BaseEvent event) {
        // Support subclasses and proxies by matching registered handler types that are assignable from the event's class
        List<DomainEventHandler<? extends BaseEvent>> matchedHandlers = new ArrayList<>();

        for (Map.Entry<Class<? extends BaseEvent>, List<DomainEventHandler<? extends BaseEvent>>> entry : handlers.entrySet()) {
            Class<? extends DomainEvent> registeredType = entry.getKey();
            if (registeredType.isAssignableFrom(event.getClass())) {
                matchedHandlers.addAll(entry.getValue());
            }
        }

        if (matchedHandlers.isEmpty()) {
            logger.debug("No handlers found for event type {}", event.getClass().getSimpleName());
            return false;
        }

        boolean anyHandled = false;
        for (DomainEventHandler<? extends BaseEvent> handler : matchedHandlers) {
            try {
                @SuppressWarnings("unchecked")
                DomainEventHandler<BaseEvent> h = (DomainEventHandler<BaseEvent>) handler;
                h.handle(event);
                anyHandled = true;
                logger.info("Dispatched event {} to handler {}", event.getClass().getSimpleName(), handler.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to handle event {} with handler {}", event.getClass().getSimpleName(), handler.getClass().getSimpleName(), e);
            }
        }

        return anyHandled;
    }

}
