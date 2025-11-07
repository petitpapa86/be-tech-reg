package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends DomainEvent {
    private final String aggregateId;
    private final String userId;
    private final String email;

    public UserRegisteredEvent(String eventId, String aggregateId, String correlationId, String causationId, String userId, String email) {
        super(correlationId, causationId);
        this.aggregateId = aggregateId;
        this.userId = userId;
        this.email = email;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }
}
