package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionTier;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command for creating a new subscription for a billing account.
 * Contains billing account ID and subscription tier information.
 */
public record CreateSubscriptionCommand(
    @NotBlank(message = "Billing account ID is required")
    String billingAccountId,
    
    @NotNull(message = "Subscription tier is required")
    SubscriptionTier tier
) {
    
    /**
     * Factory method to create and validate CreateSubscriptionCommand
     */
    public static Result<CreateSubscriptionCommand> create(String billingAccountId, SubscriptionTier tier) {
        if (billingAccountId == null || billingAccountId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_ID_REQUIRED", 
                "Billing account ID is required", "subscription.billing.account.id.required"));
        }
        
        if (tier == null) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_TIER_REQUIRED", 
                "Subscription tier is required", "subscription.tier.required"));
        }
        
        return Result.success(new CreateSubscriptionCommand(
            billingAccountId.trim(),
            tier
        ));
    }
    
    /**
     * Get BillingAccountId value object
     */
    public Result<BillingAccountId> getBillingAccountId() {
        return BillingAccountId.fromString(billingAccountId);
    }
}