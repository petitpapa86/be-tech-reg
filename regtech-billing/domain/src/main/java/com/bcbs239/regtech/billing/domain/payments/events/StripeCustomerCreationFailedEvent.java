package com.bcbs239.regtech.billing.domain.payments.events;


import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event published when Stripe customer creation fails
 */
@Getter
public class StripeCustomerCreationFailedEvent extends SagaMessage {
    
    private final String errorMessage;

    public StripeCustomerCreationFailedEvent(SagaId sagaId, String errorMessage) {
        super("StripeCustomerCreationFailedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.errorMessage = errorMessage;
    }
}

