package com.bcbs239.regtech.billing.domain.accounts.events;



import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;

import java.time.Instant;

public class BillingAccountSaveFailedEvent extends SagaMessage {

    private final String billingAccountId;
    private final String stripeCustomerId;
    private final String errorMessage;

    public BillingAccountSaveFailedEvent(SagaId sagaId, String billingAccountId, String stripeCustomerId, String errorMessage) {
        super("BillingAccountSaveFailedEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.billingAccountId = billingAccountId;
        this.stripeCustomerId = stripeCustomerId;
        this.errorMessage = errorMessage;
    }

    public String getBillingAccountId() {
        return billingAccountId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

