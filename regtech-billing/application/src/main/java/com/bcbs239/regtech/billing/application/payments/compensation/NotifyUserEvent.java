package com.bcbs239.regtech.billing.application.payments.compensation;

/**
 * Event to trigger user notification during saga compensation.
 * Published to Spring's event bus for asynchronous processing.
 */
public record NotifyUserEvent(
    String sagaId,
    String userId,
    String subject,
    String message,
    NotificationType notificationType
) {
    public enum NotificationType {
        PAYMENT_REFUNDED,
        PAYMENT_FAILED,
        SUBSCRIPTION_CANCELLED,
        SETUP_FAILED
    }
}
