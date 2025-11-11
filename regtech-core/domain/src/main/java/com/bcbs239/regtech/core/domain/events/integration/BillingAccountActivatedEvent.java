package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
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
            String userId,
            String billingAccountId,
            String subscriptionTier,
            Instant activatedAt,
            String correlationId) {
        super(correlationId, Maybe.none(), "BillingAccountActivated");
        this.userId = userId;
        this.billingAccountId = billingAccountId;
        this.subscriptionTier = subscriptionTier;
        this.activatedAt = activatedAt;
    }

    @Override
    public String eventType() {
        return "BillingAccountActivated";
    }
}