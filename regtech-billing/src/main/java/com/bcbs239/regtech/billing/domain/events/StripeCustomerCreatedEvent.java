package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class StripeCustomerCreatedEvent extends SagaMessage {

    private final String stripeCustomerId;
    private final String billingAccountId;

    public StripeCustomerCreatedEvent(SagaId sagaId, String stripeCustomerId, String billingAccountId) {
        super("StripeCustomerCreatedEvent", Instant.now(), sagaId);
        this.stripeCustomerId = stripeCustomerId;
        this.billingAccountId = billingAccountId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getBillingAccountId() {
        return billingAccountId;
    }
}