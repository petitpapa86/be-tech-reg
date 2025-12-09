package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * Local billing domain event for user registration.
 * This is the internal representation used within the billing bounded context.
 */
@Getter
@Setter
public class BillingUserRegisteredEvent extends DomainEvent {
    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    public BillingUserRegisteredEvent(String correlationId, String causationId,
                                      String userId, String email, String bankId,
                                      String paymentMethodId, String eventType) {
        super(correlationId, causationId);
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.eventType =  eventType;
    }

    @Override
    public String eventType() {
        return "BillingUserRegisteredEvent";
    }
}
