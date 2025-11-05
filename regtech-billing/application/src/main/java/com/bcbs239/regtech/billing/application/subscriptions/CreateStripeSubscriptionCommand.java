package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaId;

import java.time.Instant;
import java.util.Map;

public class CreateStripeSubscriptionCommand extends SagaCommand {

    public CreateStripeSubscriptionCommand(SagaId sagaId, String stripeCustomerId, SubscriptionTier subscriptionTier, String userId) {
        super(sagaId, "CreateStripeSubscriptionCommand", Map.of(
            "stripeCustomerId", stripeCustomerId,
            "subscriptionTier", subscriptionTier,
            "userId", userId
        ), Instant.now());
    }

    public String getStripeCustomerId() {
        return (String) payload().get("stripeCustomerId");
    }

    public SubscriptionTier getSubscriptionTier() {
        return (SubscriptionTier) payload().get("subscriptionTier");
    }

    public String getUserId() {
        return (String) payload().get("userId");
    }
}

