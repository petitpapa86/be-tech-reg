package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class PaymentMethodAttachmentFailedEvent extends SagaMessage {

    private final String stripeCustomerId;
    private final String errorMessage;

    public PaymentMethodAttachmentFailedEvent(SagaId sagaId, String stripeCustomerId, String errorMessage) {
        super("PaymentMethodAttachmentFailedEvent", Instant.now(), sagaId);
        this.stripeCustomerId = stripeCustomerId;
        this.errorMessage = errorMessage;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

