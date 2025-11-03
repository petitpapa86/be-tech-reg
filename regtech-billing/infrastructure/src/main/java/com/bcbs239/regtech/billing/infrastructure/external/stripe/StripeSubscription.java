package com.bcbs239.regtech.billing.infrastructure.external.stripe;

import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;

/**
 * DTO representing a Stripe subscription response
 */
public record StripeSubscription(
    StripeSubscriptionId subscriptionId,
    StripeCustomerId customerId,
    StripeInvoiceId latestInvoiceId,
    String status
) {
}
