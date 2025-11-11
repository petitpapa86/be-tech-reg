package com.bcbs239.regtech.billing.domain.accounts.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.UserId;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import lombok.Getter;

/**
 * Domain event published when a billing account status changes.
 * This event can be consumed by other contexts for status tracking and business logic.
 */
@Getter
public class BillingAccountStatusChangedEvent extends DomainEvent {
    
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
        super(correlationId, "BillingAccountStatusChangedEvent");
        this.billingAccountId = billingAccountId;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return "BillingAccountStatusChangedEvent";
    }
    
    @Override
    public String toString() {
        return String.format("BillingAccountStatusChangedEvent{billingAccountId=%s, userId=%s, previousStatus=%s, newStatus=%s, reason=%s, correlationId=%s}", 
            billingAccountId, userId, previousStatus, newStatus, reason, this.getCorrelationId());
    }
}

