package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.inbox.IInboxMessageRepository;
import com.bcbs239.regtech.core.domain.inbox.InboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IntegrationEventReceiver {
    private final IInboxMessageRepository inboxRepository;
    private final ObjectMapper mapper;

    public IntegrationEventReceiver(IInboxMessageRepository inboxRepository, ObjectMapper mapper) {
        this.inboxRepository = inboxRepository;
        this.mapper = mapper;
    }

    @TransactionalEventListener
    public void onIntegrationEvent(IntegrationEvent event) {
        // check for existing message to ensure idempotency could be added here

        InboxMessage entry = InboxMessage.fromIntegrationEvent(event, mapper);

        inboxRepository.save(entry);
    }
}
