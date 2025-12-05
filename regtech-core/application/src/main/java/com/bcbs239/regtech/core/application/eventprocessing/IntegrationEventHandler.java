package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for integration event handlers that ensures exactly-once processing semantics.
 * 
 * Handlers extending this class will automatically skip processing during outbox replay,
 * ensuring events are only processed once during inbox replay.
 * 
 * Usage:
 * <pre>
 * {@code
 * @Component
 * public class UserRegisteredEventHandler extends IntegrationEventHandler<BillingUserRegisteredEvent> {
 *     
 *     @EventListener
 *     public void handle(BillingUserRegisteredEvent event) {
 *         handleIntegrationEvent(event, this::processEvent);
 *     }
 *     
 *     private void processEvent(BillingUserRegisteredEvent event) {
 *         // Your business logic here
 *     }
 * }
 * }
 * </pre>
 */
public abstract class IntegrationEventHandler<T extends DomainEvent> {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * Template method that handles the outbox replay check and delegates to the actual handler.
     * 
     * @param event The domain/integration event to handle
     * @param handler The actual business logic handler
     */
    protected void handleIntegrationEvent(T event, java.util.function.Consumer<T> handler) {
        boolean isOutboxReplay = CorrelationContext.isOutboxReplay();
        boolean isInboxReplay = CorrelationContext.isInboxReplay();
        
        logger.info("Received {} (isOutboxReplay={}, isInboxReplay={})", 
                event.getClass().getSimpleName(), isOutboxReplay, isInboxReplay);
        
        // Skip processing if from outbox replay - will be processed via inbox
        if (isOutboxReplay && !isInboxReplay) {
            logger.info("Skipping processing - event from outbox replay will be processed via inbox: {}", 
                    event.getClass().getSimpleName());
            return;
        }
        
        // Process the event
        handler.accept(event);
    }
}
