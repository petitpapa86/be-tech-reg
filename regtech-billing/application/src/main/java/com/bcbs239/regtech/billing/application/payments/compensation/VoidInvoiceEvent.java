package com.bcbs239.regtech.billing.application.payments.compensation;

/**
 * Event to trigger invoice voiding during saga compensation.
 * Published to Spring's event bus for asynchronous processing.
 */
public record VoidInvoiceEvent(
    String sagaId,
    String stripeInvoiceId,
    String userId,
    String reason
) {
}
