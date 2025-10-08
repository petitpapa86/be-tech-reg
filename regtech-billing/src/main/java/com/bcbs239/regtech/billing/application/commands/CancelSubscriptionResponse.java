package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionStatus;
import java.time.LocalDate;

/**
 * Response for successful subscription cancellation.
 * Contains the cancelled subscription details and effective cancellation date.
 */
public record CancelSubscriptionResponse(
    SubscriptionId subscriptionId,
    SubscriptionStatus status,
    LocalDate cancellationDate,
    String message
) {
    
    /**
     * Factory method to create CancelSubscriptionResponse
     */
    public static CancelSubscriptionResponse of(
            SubscriptionId subscriptionId,
            SubscriptionStatus status,
            LocalDate cancellationDate) {
        return new CancelSubscriptionResponse(
            subscriptionId,
            status,
            cancellationDate,
            String.format("Subscription cancelled successfully. Service will end on %s.", cancellationDate)
        );
    }
}