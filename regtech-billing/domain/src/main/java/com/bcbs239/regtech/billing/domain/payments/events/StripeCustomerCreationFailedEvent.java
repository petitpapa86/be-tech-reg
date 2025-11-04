package com.bcbs239.regtech.billing.domain.payments.events;

import com.bcbs239.regtech.core.saga.SagaId;

/**
 * Domain event published when Stripe customer creation fails
 */
public record StripeCustomerCreationFailedEvent(
    SagaId sagaId,
    String errorMessage
) {
}