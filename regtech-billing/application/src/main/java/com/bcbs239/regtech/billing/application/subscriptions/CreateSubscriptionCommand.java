package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.shared.validation.BillingValidationUtils;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Command for creating a new subscription for a billing account.
 * Contains billing account ID and subscription tier information.
 */
public record CreateSubscriptionCommand(
    @NotBlank(message = "Billing account ID is required")
    @Size(min = 3, max = 50, message = "Billing account ID must be between 3 and 50 characters")
    String billingAccountId,
    
    @NotNull(message = "Subscription tier is required")
    SubscriptionTier tier
) {
    
    /**
     * Factory method to create and validate CreateSubscriptionCommand
     */
    public static Result<CreateSubscriptionCommand> create(String billingAccountId, SubscriptionTier tier) {
        // Sanitize input
        String sanitizedBillingAccountId = BillingValidationUtils.sanitizeStringInput(billingAccountId);
        
        // Validate billing account ID
        Result<Void> billingAccountIdValidation = BillingValidationUtils.validateBillingAccountId(sanitizedBillingAccountId);
        if (billingAccountIdValidation.isFailure()) {
            return Result.failure(billingAccountIdValidation.getError().get());
        }
        
        if (tier == null) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_TIER_REQUIRED", 
                "Subscription tier is required", "subscription.tier.required"));
        }
        
        return Result.success(new CreateSubscriptionCommand(
            sanitizedBillingAccountId,
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
