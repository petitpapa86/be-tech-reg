package com.bcbs239.regtech.billing.domain.payments.events;



import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

@Getter
public class StripePaymentFailedEvent extends SagaMessage {

    private final String failureReason;

    public StripePaymentFailedEvent(SagaId sagaId, String failureReason) {
        super("StripePaymentFailedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.failureReason = failureReason;
    }

}

