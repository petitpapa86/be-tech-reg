package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionStatus;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

import java.time.LocalDate;

/**
 * Response for successful subscription creation.
 * Contains the created subscription details and pricing information.
 */
public record CreateSubscriptionResponse(
    SubscriptionId subscriptionId,
    SubscriptionTier tier,
    SubscriptionStatus status,
    Money monthlyAmount,
    LocalDate startDate,
    LocalDate nextBillingDate
) {
    
    /**
     * Factory method to create CreateSubscriptionResponse
     */
    public static CreateSubscriptionResponse of(
            SubscriptionId subscriptionId,
            SubscriptionTier tier,
            SubscriptionStatus status,
            Money monthlyAmount,
            LocalDate startDate,
            LocalDate nextBillingDate) {
        return new CreateSubscriptionResponse(
            subscriptionId,
            tier,
            status,
            monthlyAmount,
            startDate,
            nextBillingDate
        );
    }
}
