package com.bcbs239.regtech.billing.application.integration;

/**
 * Data class for webhook signature verification.
 * Contains the payload and signature header needed for Stripe webhook verification.
 */
public record WebhookVerificationData(
    String payload,
    String signatureHeader
) {
}

