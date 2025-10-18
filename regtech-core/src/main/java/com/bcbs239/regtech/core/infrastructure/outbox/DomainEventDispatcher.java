package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
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
import java.util.function.Function;

/**
 * Dispatcher for domain events using closures.
 * Registers handlers as functions and dispatches events to matching handlers.
 */
@Component
public class DomainEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventDispatcher.class);

    private final Map<Class<? extends DomainEvent>, List<Function<DomainEvent, Boolean>>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public DomainEventDispatcher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Register handlers from Spring context
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(DomainEventHandler.class);
        if (beans != null && !beans.isEmpty()) {
            for (Object bean : beans.values()) {
                try {
                    if (bean instanceof Function) {
                        @SuppressWarnings("unchecked")
                        Function<DomainEvent, Boolean> handlerFunction = (Function<DomainEvent, Boolean>) bean;
                        // For now, assume all handlers handle all events or use annotation to specify
                        // This is simplified; in practice, we'd need a way to map handlers to event types
                        registerHandler(DomainEvent.class, handlerFunction);
                    }
                } catch (Exception e) {
                    logger.error("Failed to auto-register domain event handler {}", bean.getClass().getName(), e);
                }
            }
        } else {
            logger.debug("No DomainEventHandler beans found during context refresh.");
        }
    }

    /**
     * Register a handler function for a specific event type.
     */
    public void registerHandler(Class<? extends DomainEvent> eventType, Function<DomainEvent, Boolean> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        logger.info("Registered event handler for event type {}", eventType.getSimpleName());
    }

    /**
     * Dispatch event to all matching handlers.
     */
    public void dispatch(DomainEvent event) {
        List<Function<DomainEvent, Boolean>> matchedHandlers = new ArrayList<>();

        // Find handlers for exact type and super types
        for (Map.Entry<Class<? extends DomainEvent>, List<Function<DomainEvent, Boolean>>> entry : handlers.entrySet()) {
            Class<? extends DomainEvent> registeredType = entry.getKey();
            if (registeredType.isAssignableFrom(event.getClass())) {
                matchedHandlers.addAll(entry.getValue());
            }
        }

        if (!matchedHandlers.isEmpty()) {
            for (Function<DomainEvent, Boolean> handler : matchedHandlers) {
                try {
                    boolean success = handler.apply(event);
                    if (success) {
                        logger.info("Dispatched event {} successfully", event.getClass().getSimpleName());
                    } else {
                        logger.warn("Handler returned false for event {}", event.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    logger.error("Failed to dispatch event {} to handler", event.getClass().getSimpleName(), e);
                }
            }
        } else {
            logger.debug("No handlers found for event type {}", event.getClass().getSimpleName());
        }
    }

    /**
     * Annotation for domain event handlers.
     */
    @interface DomainEventHandler {
    }
}