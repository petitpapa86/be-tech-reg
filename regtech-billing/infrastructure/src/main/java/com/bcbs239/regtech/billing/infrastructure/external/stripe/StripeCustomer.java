package com.bcbs239.regtech.billing.infrastructure.external.stripe;


import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;

/**
 * DTO representing a Stripe customer response
 */
public record StripeCustomer(
    StripeCustomerId customerId,
    String email,
    String name
) {
}
