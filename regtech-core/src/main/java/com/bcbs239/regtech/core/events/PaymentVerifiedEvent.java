package com.bcbs239.regtech.core.events;

/**
 * Cross-module event published when payment is successfully verified.
 * This event is consumed by the IAM context to activate the user account.
 */
public class PaymentVerifiedEvent extends BaseEvent {
    
    private final String userId;
    private final String billingAccountId;
    
    public PaymentVerifiedEvent(String userId, String billingAccountId, String correlationId) {
        super(correlationId, "billing");
        this.userId = userId;
        this.billingAccountId = billingAccountId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getBillingAccountId() {
        return billingAccountId;
    }
    
    @Override
    public String toString() {
        return String.format("PaymentVerifiedEvent{userId=%s, billingAccountId=%s, correlationId=%s}", 
            userId, billingAccountId, getCorrelationId());
    }
}