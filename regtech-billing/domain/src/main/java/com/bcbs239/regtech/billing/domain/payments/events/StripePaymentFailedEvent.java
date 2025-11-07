package com.bcbs239.regtech.billing.domain.payments.events;



import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;

import java.time.Instant;

public class StripePaymentFailedEvent extends SagaMessage {

    private final String failureReason;

    public StripePaymentFailedEvent(SagaId sagaId, String failureReason) {
        super("StripePaymentFailedEvent", Instant.now(), sagaId);
        this.failureReason = failureReason;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

