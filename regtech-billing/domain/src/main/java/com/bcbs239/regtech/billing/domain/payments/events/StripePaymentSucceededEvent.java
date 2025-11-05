package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class StripePaymentSucceededEvent extends SagaMessage {

    private final String stripePaymentIntentId;

    public StripePaymentSucceededEvent(SagaId sagaId, String stripePaymentIntentId) {
        super("StripePaymentSucceededEvent", Instant.now(), sagaId);
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }
}

