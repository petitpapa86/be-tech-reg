package com.bcbs239.regtech.billing.infrastructure.configuration;

import java.time.ZoneId;

/**
 * Type-safe configuration for billing cycle settings.
 * Defines timezone and billing day for monthly billing cycles.
 */
public record BillingCycleConfiguration(
    ZoneId timezone,
    int billingDay
) {
    
    /**
     * Gets the timezone for billing operations
     */
    public ZoneId getTimezone() {
        return timezone;
    }
    
    /**
     * Gets the day of month for billing (1-28)
     */
    public int getBillingDay() {
        return billingDay;
    }
    
    /**
     * Validates billing cycle configuration
     */
    public void validate() {
        if (timezone == null) {
            throw new IllegalStateException("Billing timezone is required");
        }
        if (billingDay < 1 || billingDay > 28) {
            throw new IllegalStateException("Billing day must be between 1 and 28 to ensure it exists in all months");
        }
    }
}

