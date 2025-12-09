package com.bcbs239.regtech.billing.domain.subscriptions.events;


import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

@Getter
public class StripeSubscriptionWebhookReceivedEvent extends SagaMessage {

    private final String stripeSubscriptionId;
    private final String stripeInvoiceId;

    public StripeSubscriptionWebhookReceivedEvent(SagaId sagaId, String stripeSubscriptionId, String stripeInvoiceId) {
        super("StripeSubscriptionWebhookReceivedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeInvoiceId = stripeInvoiceId;
    }

}

