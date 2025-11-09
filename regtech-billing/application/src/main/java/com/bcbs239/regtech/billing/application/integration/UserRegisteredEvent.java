package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends DomainEvent {
    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    public UserRegisteredEvent(String correlationId, String causationId, String userId, String email, String bankId, String paymentMethodId) {
        super(correlationId, causationId);
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.userId = userId;
        this.email = email;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }
}
