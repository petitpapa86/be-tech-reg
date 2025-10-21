package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deprecated compatibility wrapper for the new IntegrationEventHandlerRegistry.
 * Keeps the old type available for modules that still depend on it and delegates
 * all operations to the registry.
 */
@Component
@Deprecated
public class IntegrationEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventDispatcher.class);

    private final IntegrationEventHandlerRegistry delegate;

    public IntegrationEventDispatcher(IntegrationEventHandlerRegistry delegate) {
        this.delegate = delegate;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        // Delegate to new registry (it already implements registration logic)
        try {
            delegate.onContextRefreshed(event);
        } catch (Exception e) {
            logger.error("Error delegating context refresh to IntegrationEventHandlerRegistry", e);
        }
    }

    public void registerHandler(DomainEventHandler<? extends DomainEvent> handler) {
        delegate.registerHandler(handler);
    }

    public List<DomainEventHandler<? extends DomainEvent>> getHandlers(Class<? extends DomainEvent> eventType) {
        return delegate.getHandlers(eventType);
    }
}