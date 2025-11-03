package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class StripeSubscriptionWebhookReceivedEvent extends SagaMessage {

    private final String stripeSubscriptionId;
    private final String stripeInvoiceId;

    public StripeSubscriptionWebhookReceivedEvent(SagaId sagaId, String stripeSubscriptionId, String stripeInvoiceId) {
        super("StripeSubscriptionWebhookReceivedEvent", Instant.now(), sagaId);
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeInvoiceId = stripeInvoiceId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }
}