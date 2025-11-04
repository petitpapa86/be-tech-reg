package com.bcbs239.regtech.billing.infrastructure.external.stripe;

import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;

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
