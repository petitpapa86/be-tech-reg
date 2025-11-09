package com.bcbs239.regtech.billing.domain.accounts.events;



import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.Getter;

import java.time.Instant;

@Getter
public class BillingAccountNotFoundEvent extends SagaMessage {

    private final String billingAccountId;
    private final String stripeCustomerId;

    public BillingAccountNotFoundEvent(SagaId sagaId, String billingAccountId, String stripeCustomerId) {
        super("BillingAccountNotFoundEvent", Instant.now(), sagaId, sagaId.toString(), sagaId.toString());
        this.billingAccountId = billingAccountId;
        this.stripeCustomerId = stripeCustomerId;
    }

}

