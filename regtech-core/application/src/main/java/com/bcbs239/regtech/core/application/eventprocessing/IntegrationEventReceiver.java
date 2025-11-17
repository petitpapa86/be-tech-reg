package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
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
        logger.info("📨 IntegrationEventReceiver: event={}, isInboxReplay={}", 
                event.getClass().getSimpleName(), isInboxReplay);
        
        if (isInboxReplay) {
            logger.info("✅ Skipping inbox save (replay): {}", event.getClass().getSimpleName());
            return;
        }
        
        // check for existing message to ensure idempotency could be added here
        try {
            logger.info("💾 Saving to inbox: {}", event.getClass().getSimpleName());
            InboxMessage entry = InboxMessage.fromIntegrationEvent(event, mapper);
            inboxRepository.save(entry);
            logger.debug("Saved inbox message for eventId={}", entry.getId());
        } catch (Exception ex) {
            logger.error("Failed to persist inbox message for integration event {}", event.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}
