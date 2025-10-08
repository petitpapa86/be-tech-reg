package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import java.time.LocalDate;

/**
 * Response for subscription details retrieval.
 * Contains complete subscription information including status, tier, and dates.
 */
public record GetSubscriptionResponse(
    SubscriptionId subscriptionId,
    BillingAccountId billingAccountId,
    SubscriptionTier tier,
    SubscriptionStatus status,
    Money monthlyAmount,
    int exposureLimit,
    LocalDate startDate,
    LocalDate endDate,
    boolean isActive,
    boolean isValid,
    boolean requiresPaymentAttention
) {
    
    /**
     * Factory method to create GetSubscriptionResponse from Subscription aggregate
     */
    public static GetSubscriptionResponse from(com.bcbs239.regtech.billing.domain.subscriptions.Subscription subscription) {
        return new GetSubscriptionResponse(
            subscription.getId(),
            subscription.getBillingAccountId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getMonthlyAmount(),
            subscription.getExposureLimit(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.isActive(),
            subscription.isValid(),
            subscription.requiresPaymentAttention()
        );
    }
}