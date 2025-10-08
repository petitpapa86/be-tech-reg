package com.bcbs239.regtech.core.events;

/**
 * Cross-module event published when a billing account status changes.
 * This event can be consumed by other contexts for status tracking and business logic.
 */
public class BillingAccountStatusChangedEvent extends BaseEvent {
    
    private final String billingAccountId;
    private final String userId;
    private final String previousStatus;
    private final String newStatus;
    private final String reason;
    
    public BillingAccountStatusChangedEvent(String billingAccountId, 
                                          String userId,
                                          String previousStatus, 
                                          String newStatus,
                                          String reason,
                                          String correlationId) {
        super(correlationId, "billing");
        this.billingAccountId = billingAccountId;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
    
    public String getBillingAccountId() {
        return billingAccountId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return String.format("BillingAccountStatusChangedEvent{billingAccountId=%s, userId=%s, previousStatus=%s, newStatus=%s, reason=%s, correlationId=%s}", 
            billingAccountId, userId, previousStatus, newStatus, reason, getCorrelationId());
    }
}