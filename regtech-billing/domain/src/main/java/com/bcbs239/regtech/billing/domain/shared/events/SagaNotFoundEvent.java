package com.bcbs239.regtech.billing.domain.shared.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class SagaNotFoundEvent extends SagaMessage {

    private final String stripeCustomerId;

    public SagaNotFoundEvent(SagaId sagaId, String stripeCustomerId) {
        super("SagaNotFoundEvent", Instant.now(), sagaId);
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }
}