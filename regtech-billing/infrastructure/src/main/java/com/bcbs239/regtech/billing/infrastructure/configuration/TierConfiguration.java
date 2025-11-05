package com.bcbs239.regtech.billing.infrastructure.configuration;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Type-safe configuration for subscription tier settings.
 * Currently supports STARTER tier configuration.
 */
public record TierConfiguration(
    BigDecimal starterMonthlyPrice,
    String starterCurrency,
    int starterExposureLimit
) {
    
    /**
     * Gets the monthly price for STARTER tier as a Money-like object
     */
    public BigDecimal getStarterMonthlyPrice() {
        return starterMonthlyPrice;
    }
    
    /**
     * Gets the currency for STARTER tier
     */
    public Currency getStarterCurrency() {
        return Currency.getInstance(starterCurrency);
    }
    
    /**
     * Gets the exposure limit for STARTER tier
     */
    public int getStarterExposureLimit() {
        return starterExposureLimit;
    }
    
    /**
     * Validates tier configuration
     */
    public void validate() {
        if (starterMonthlyPrice == null || starterMonthlyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("STARTER tier monthly price must be positive");
        }
        if (starterCurrency == null || starterCurrency.isBlank()) {
            throw new IllegalStateException("STARTER tier currency is required");
        }
        if (starterExposureLimit <= 0) {
            throw new IllegalStateException("STARTER tier exposure limit must be positive");
        }
        
        try {
            Currency.getInstance(starterCurrency);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid currency code: " + starterCurrency, e);
        }
    }
}

