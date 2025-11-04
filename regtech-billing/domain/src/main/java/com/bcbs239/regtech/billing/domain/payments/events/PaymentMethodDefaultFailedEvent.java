package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class PaymentMethodDefaultFailedEvent extends SagaMessage {

    private final String stripeCustomerId;
    private final String paymentMethodId;
    private final String errorMessage;

    public PaymentMethodDefaultFailedEvent(SagaId sagaId, String stripeCustomerId, String paymentMethodId, String errorMessage) {
        super("PaymentMethodDefaultFailedEvent", Instant.now(), sagaId);
        this.stripeCustomerId = stripeCustomerId;
        this.paymentMethodId = paymentMethodId;
        this.errorMessage = errorMessage;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}