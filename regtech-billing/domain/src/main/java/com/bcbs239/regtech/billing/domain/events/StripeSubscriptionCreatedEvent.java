package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;
import java.util.Optional;

public class StripeSubscriptionCreatedEvent extends SagaMessage {

    private final String stripeSubscriptionId;
    private final String stripeInvoiceId;
    private final Optional<SubscriptionId> subscriptionId;

    public StripeSubscriptionCreatedEvent(SagaId sagaId, String stripeSubscriptionId, String stripeInvoiceId, SubscriptionId subscriptionId) {
        super("StripeSubscriptionCreatedEvent", Instant.now(), sagaId);
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeInvoiceId = stripeInvoiceId;
        this.subscriptionId = Optional.ofNullable(subscriptionId);
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public Optional<SubscriptionId> getSubscriptionId() {
        return subscriptionId;
    }
}