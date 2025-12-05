package com.bcbs239.regtech.billing.domain.subscriptions.events;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

@Getter
public class StripeSubscriptionCreatedEvent extends SagaMessage {

    private final String stripeSubscriptionId;
    private final String stripeInvoiceId;
    private final Optional<SubscriptionId> subscriptionId;

    public StripeSubscriptionCreatedEvent(SagaId sagaId, String stripeSubscriptionId, String stripeInvoiceId, SubscriptionId subscriptionId) {
        super("StripeSubscriptionCreatedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeInvoiceId = stripeInvoiceId;
        this.subscriptionId = Optional.ofNullable(subscriptionId);
    }

}

