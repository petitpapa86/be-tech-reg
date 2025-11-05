package com.bcbs239.regtech.core.domain.events;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Cross-module event published when a subscription is cancelled.
 * This event can be consumed by other contexts for cleanup processes and business logic.
 */
@Getter
public class SubscriptionCancelledEvent extends BaseEvent {
    
    private final String subscriptionId;
    private final String billingAccountId;
    private final String userId;
    private final String tier;
    private final LocalDate cancellationDate;
    private final String cancellationReason;
    
    public SubscriptionCancelledEvent(String subscriptionId,
                                    String billingAccountId,
                                    String userId,
                                    String tier,
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

    @Override
    public String toString() {
        return String.format("SubscriptionCancelledEvent{subscriptionId=%s, billingAccountId=%s, userId=%s, tier=%s, cancellationDate=%s, reason=%s, correlationId=%s}", 
            subscriptionId, billingAccountId, userId, tier, cancellationDate, cancellationReason, getCorrelationId());
    }
}
