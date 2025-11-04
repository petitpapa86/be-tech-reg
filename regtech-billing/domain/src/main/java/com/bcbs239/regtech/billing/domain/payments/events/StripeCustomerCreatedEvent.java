package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

@Getter
public class StripeCustomerCreatedEvent extends SagaMessage {

    private final String stripeCustomerId;

    public StripeCustomerCreatedEvent(SagaId sagaId, String stripeCustomerId) {
        super("StripeCustomerCreatedEvent", Instant.now(), sagaId);
        this.stripeCustomerId = stripeCustomerId;
    }

}