package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class StripeInvoiceCreatedEvent extends SagaMessage {

    private final String stripeInvoiceId;

    public StripeInvoiceCreatedEvent(SagaId sagaId, String stripeInvoiceId) {
        super("StripeInvoiceCreatedEvent", Instant.now(), sagaId);
        this.stripeInvoiceId = stripeInvoiceId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }
}