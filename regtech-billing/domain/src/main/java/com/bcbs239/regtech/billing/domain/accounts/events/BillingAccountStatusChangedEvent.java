package com.bcbs239.regtech.billing.domain.accounts.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountStatus;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Domain event published when a billing account status changes.
 * This event can be consumed by other contexts for status tracking and business logic.
 */
public class BillingAccountStatusChangedEvent extends BaseEvent {
    
    private final BillingAccountId billingAccountId;
    private final UserId userId;
    private final BillingAccountStatus previousStatus;
    private final BillingAccountStatus newStatus;
    private final String reason;
    
    public BillingAccountStatusChangedEvent(BillingAccountId billingAccountId, 
                                          UserId userId,
                                          BillingAccountStatus previousStatus, 
                                          BillingAccountStatus newStatus,
                                          String reason,
                                          String correlationId) {
        super(correlationId, "billing");
        this.billingAccountId = billingAccountId;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
    
    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public BillingAccountStatus getPreviousStatus() {
        return previousStatus;
    }
    
    public BillingAccountStatus getNewStatus() {
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

