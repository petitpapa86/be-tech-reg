package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.time.LocalDate;

/**
 * Domain event published when a subscription is cancelled.
 * This event can be consumed by other contexts for cleanup processes and business logic.
 */
public class SubscriptionCancelledEvent extends BaseEvent {
    
    private final SubscriptionId subscriptionId;
    private final BillingAccountId billingAccountId;
    private final UserId userId;
    private final SubscriptionTier tier;
    private final LocalDate cancellationDate;
    private final String cancellationReason;
    
    public SubscriptionCancelledEvent(SubscriptionId subscriptionId,
                                    BillingAccountId billingAccountId,
                                    UserId userId,
                                    SubscriptionTier tier,
                                    LocalDate cancellationDate,
                                    String cancellationReason,
                                    String correlationId) {
        super(correlationId, "billing");
        this.subscriptionId = subscriptionId;
        this.billingAccountId = billingAccountId;
        this.userId = userId;
        this.tier = tier;
        this.cancellationDate = cancellationDate;
        this.cancellationReason = cancellationReason;
    }
    
    public SubscriptionId getSubscriptionId() {
        return subscriptionId;
    }
    
    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }
    
    public UserId getUserId() {
        return userId;
    }
    
    public SubscriptionTier getTier() {
        return tier;
    }
    
    public LocalDate getCancellationDate() {
        return cancellationDate;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    @Override
    public String toString() {
        return String.format("SubscriptionCancelledEvent{subscriptionId=%s, billingAccountId=%s, userId=%s, tier=%s, cancellationDate=%s, reason=%s, correlationId=%s}", 
            subscriptionId, billingAccountId, userId, tier, cancellationDate, cancellationReason, getCorrelationId());
    }
}

