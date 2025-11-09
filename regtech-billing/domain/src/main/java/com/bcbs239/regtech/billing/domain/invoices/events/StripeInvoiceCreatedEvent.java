package com.bcbs239.regtech.billing.domain.invoices.events;



import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;

import java.time.Instant;

public class StripeInvoiceCreatedEvent extends SagaMessage {

    private final String stripeInvoiceId;

    public StripeInvoiceCreatedEvent(SagaId sagaId, String stripeInvoiceId) {
        super("StripeInvoiceCreatedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.stripeInvoiceId = stripeInvoiceId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }
}

