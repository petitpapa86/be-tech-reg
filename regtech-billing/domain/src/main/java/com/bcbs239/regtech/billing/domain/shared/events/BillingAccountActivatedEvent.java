package com.bcbs239.regtech.billing.domain.shared.events;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.billing.domain.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;

/**
 * Integration event published when a billing account is successfully activated.
 * This event is consumed by the IAM module to update user registration status.
 */
@Getter
public class BillingAccountActivatedEvent extends IntegrationEvent {
    
    private final String userId;
    private final String billingAccountId;
    private final String subscriptionTier;
    private final Instant activatedAt;
    
    public BillingAccountActivatedEvent(
            UserId userId,
            BillingAccountId billingAccountId,
            SubscriptionTier subscriptionTier,
            Instant activatedAt,
            String correlationId) {
        super(correlationId, Maybe.none(), "BillingAccountActivated");
        this.userId = userId.value().toString();
        this.billingAccountId = billingAccountId.value();
        this.subscriptionTier = subscriptionTier.name();
        this.activatedAt = activatedAt;
    }
    
    @Override
    public String eventType() {
        return "BillingAccountActivated";
    }
}
