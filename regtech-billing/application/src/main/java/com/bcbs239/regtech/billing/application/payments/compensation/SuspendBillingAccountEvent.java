package com.bcbs239.regtech.billing.application.payments.compensation;

/**
 * Event to trigger billing account suspension during saga compensation.
 * Published to Spring's event bus for asynchronous processing.
 */
public record SuspendBillingAccountEvent(
    String sagaId,
    String billingAccountId,
    String userId,
    String reason
) {
}
