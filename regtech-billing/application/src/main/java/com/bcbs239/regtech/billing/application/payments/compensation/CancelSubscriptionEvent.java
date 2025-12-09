package com.bcbs239.regtech.billing.application.payments.compensation;

/**
 * Event to trigger subscription cancellation during saga compensation.
 * Published to Spring's event bus for asynchronous processing.
 */
public record CancelSubscriptionEvent(
    String sagaId,
    String stripeSubscriptionId,
    String userId,
    String reason
) {
}
