package com.bcbs239.regtech.billing.application.createsubscription;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.validation.BillingValidationUtils;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Command to create a new subscription for a billing account.
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
     * Get billing account ID (compatibility method)
     */
    public String getBillingAccountId() {
        return billingAccountId;
    }

    /**
     * Factory method to create and validate CreateSubscriptionCommand
     */
    public static Result<CreateSubscriptionCommand> create(String billingAccountId, SubscriptionTier tier) {
        // Sanitize input
        String sanitizedBillingAccountId = BillingValidationUtils.sanitizeStringInput(billingAccountId);
        
        // Validate billing account ID
        Result<Void> billingAccountValidation = BillingValidationUtils.validateBillingAccountId(sanitizedBillingAccountId);
        if (billingAccountValidation.isFailure()) {
            return Result.failure(billingAccountValidation.getError().get());
        }
        
        // Validate tier
        if (tier == null) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_TIER_REQUIRED",
                "Subscription tier is required", "validation.subscription.tier.required"));
        }
        
        return Result.success(new CreateSubscriptionCommand(
            sanitizedBillingAccountId,
            tier
        ));
    }
}