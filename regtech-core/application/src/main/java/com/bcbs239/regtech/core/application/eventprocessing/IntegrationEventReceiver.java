package com.bcbs239.regtech.core.application.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IntegrationEventReceiver {
    private final IInboxMessageRepository inboxRepository;
    private final ObjectMapper mapper;

    public IntegrationEventReceiver(IInboxMessageRepository inboxRepository, ObjectMapper mapper) {
        this.inboxRepository = inboxRepository;
        this.mapper = mapper;
    }

    @EventListener
    public void onIntegrationEvent(IntegrationEvent event) {
        // check for existing message to ensure idempotency could be added here

        InboxMessage entry = InboxMessage.fromIntegrationEvent(event, mapper);

        inboxRepository.save(entry);
    }
}
