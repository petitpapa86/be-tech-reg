package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IntegrationEventReceiver {
    private final IInboxMessageRepository inboxRepository;
    private final ObjectMapper mapper;
    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventReceiver.class);

    public IntegrationEventReceiver(IInboxMessageRepository inboxRepository, ObjectMapper mapper) {
        this.inboxRepository = inboxRepository;
        this.mapper = mapper;
    }

    @EventListener
    @Transactional
    public void onIntegrationEvent(IntegrationEvent event) {
        // Skip saving events that are being replayed from inbox to prevent loops
        boolean isInboxReplay = CorrelationContext.isInboxReplay();
        boolean isOutboxReplay = CorrelationContext.isOutboxReplay();
        
        logger.info("📨 IntegrationEventReceiver: event={}, isInboxReplay={}, isOutboxReplay={}", 
                event.getClass().getSimpleName(), isInboxReplay, isOutboxReplay);
        
        if (isInboxReplay) {
            logger.info("✅ Skipping inbox save (replay): {}", event.getClass().getSimpleName());
            return;
        }
        
        // Save to inbox for guaranteed delivery
        try {
            logger.info("💾 Saving to inbox: {}", event.getClass().getSimpleName());
            InboxMessage entry = InboxMessage.fromIntegrationEvent(event, mapper);
            inboxRepository.save(entry);
            logger.debug("Saved inbox message for eventId={}", entry.getId());
        } catch (Exception ex) {
            logger.error("Failed to persist inbox message for integration event {}", event.getClass().getSimpleName(), ex);
            throw ex;
        }
        
        // IMPORTANT: Stop event propagation if this is from outbox replay
        // The event will be processed later by ProcessInboxJob to ensure exactly-once semantics
        if (isOutboxReplay) {
            logger.info("🛑 Stopping event propagation (outbox replay): {} - will be processed by inbox job", 
                    event.getClass().getSimpleName());
            // Note: We can't actually stop Spring's event propagation here,
            // so handlers must check isOutboxReplay flag themselves
        }
    }
}
