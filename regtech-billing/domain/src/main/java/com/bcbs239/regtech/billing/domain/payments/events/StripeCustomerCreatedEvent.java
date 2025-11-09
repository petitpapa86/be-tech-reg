package com.bcbs239.regtech.billing.domain.payments.events;


import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

@Getter
public class StripeCustomerCreatedEvent extends SagaMessage {

    private final String stripeCustomerId;

    public StripeCustomerCreatedEvent(SagaId sagaId, String stripeCustomerId) {
        super("StripeCustomerCreatedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.stripeCustomerId = stripeCustomerId;
    }

}

