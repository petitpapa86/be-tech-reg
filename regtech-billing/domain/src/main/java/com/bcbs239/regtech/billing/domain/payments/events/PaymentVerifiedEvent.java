package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;
import lombok.Getter;

/**
 * Domain event published when payment is successfully verified.
 * This event is consumed by the IAM context to activate the user account.
 */
@Getter
public class PaymentVerifiedEvent extends DomainEvent {
    
    private final UserId userId;
    private final BillingAccountId billingAccountId;
    
    public PaymentVerifiedEvent(UserId userId, BillingAccountId billingAccountId, String correlationId) {
        super(correlationId, "PaymentVerifiedEvent");
        this.userId = userId;
        this.billingAccountId = billingAccountId;
    }

    @Override
    public String eventType() {
        return "PaymentVerifiedEvent";
    }
    
    @Override
    public String toString() {
        return String.format("PaymentVerifiedEvent{userId=%s, billingAccountId=%s, correlationId=%s}", 
            userId, billingAccountId, getCorrelationId());
    }
}

