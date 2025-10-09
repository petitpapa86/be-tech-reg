package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Domain event published when payment is successfully verified.
 * This event is consumed by the IAM context to activate the user account.
 */
public class PaymentVerifiedEvent extends BaseEvent {
    
    private final UserId userId;
    private final BillingAccountId billingAccountId;
    
    public PaymentVerifiedEvent(UserId userId, BillingAccountId billingAccountId, String correlationId) {
        super(correlationId, "billing");
        this.userId = userId;
        this.billingAccountId = billingAccountId;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }
    
    @Override
    public String toString() {
        return String.format("PaymentVerifiedEvent{userId=%s, billingAccountId=%s, correlationId=%s}", 
            userId, billingAccountId, getCorrelationId());
    }
}
