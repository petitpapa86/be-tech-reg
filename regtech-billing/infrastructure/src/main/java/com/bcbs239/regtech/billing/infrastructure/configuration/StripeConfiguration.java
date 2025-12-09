package com.bcbs239.regtech.billing.infrastructure.configuration;

/**
 * Type-safe configuration for Stripe API settings.
 * Provides access to Stripe API key and webhook secret.
 */
public record StripeConfiguration(
    String apiKey,
    String webhookSecret
) {
    
    /**
     * Validates that required Stripe configuration is present
     */
    public void validate() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Stripe API key is required but not configured");
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is required but not configured");
        }
        if (apiKey.contains("placeholder")) {
            throw new IllegalStateException("Stripe API key appears to be a placeholder value");
        }
        if (webhookSecret.contains("placeholder")) {
            throw new IllegalStateException("Stripe webhook secret appears to be a placeholder value");
        }
    }
    
    /**
     * Checks if this is a test environment configuration
     */
    public boolean isTestMode() {
        return apiKey.startsWith("sk_test_");
    }
    
    /**
     * Checks if this is a live environment configuration
     */
    public boolean isLiveMode() {
        return apiKey.startsWith("sk_live_");
    }
}

