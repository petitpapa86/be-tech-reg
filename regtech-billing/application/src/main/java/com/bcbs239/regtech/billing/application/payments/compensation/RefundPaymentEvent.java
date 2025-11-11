package com.bcbs239.regtech.billing.application.payments.compensation;

/**
 * Event to trigger payment refund during saga compensation.
 * Published to Spring's event bus for asynchronous processing.
 */
public record RefundPaymentEvent(
    String sagaId,
    String stripePaymentIntentId,
    String userId,
    String reason
) {
}
