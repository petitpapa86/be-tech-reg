package com.bcbs239.regtech.billing.domain.accounts.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class BillingAccountNotFoundEvent extends SagaMessage {

    private final String billingAccountId;
    private final String stripeCustomerId;

    public BillingAccountNotFoundEvent(SagaId sagaId, String billingAccountId, String stripeCustomerId) {
        super("BillingAccountNotFoundEvent", Instant.now(), sagaId);
        this.billingAccountId = billingAccountId;
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getBillingAccountId() {
        return billingAccountId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }
}