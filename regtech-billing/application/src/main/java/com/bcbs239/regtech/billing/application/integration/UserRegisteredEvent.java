package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.domain.events.BaseEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseEvent {
    private final String eventId;
    private final String aggregateId;
    private final String correlationId;
    private final String causationId;
    private final String userId;
    private final String email;

    public UserRegisteredEvent(String eventId, String aggregateId, String correlationId, String causationId, String userId, String email) {
        super(correlationId,eventId,aggregateId,causationId);
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.userId = userId;
        this.email = email;
    }
}
