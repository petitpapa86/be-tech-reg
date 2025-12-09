package com.bcbs239.regtech.billing.infrastructure.configuration;

/**
 * Type-safe configuration for dunning process settings.
 * Defines timing intervals for dunning reminders and final actions.
 */
public record DunningConfiguration(
    int step1IntervalDays,
    int step2IntervalDays,
    int step3IntervalDays,
    int finalActionDelayDays
) {
    
    /**
     * Gets the number of days after due date for first reminder
     */
    public int getStep1IntervalDays() {
        return step1IntervalDays;
    }
    
    /**
     * Gets the number of days after step 1 for second reminder
     */
    public int getStep2IntervalDays() {
        return step2IntervalDays;
    }
    
    /**
     * Gets the number of days after step 2 for third reminder
     */
    public int getStep3IntervalDays() {
        return step3IntervalDays;
    }
    
    /**
     * Gets the number of days after step 3 before final action (suspension)
     */
    public int getFinalActionDelayDays() {
        return finalActionDelayDays;
    }
    
    /**
     * Gets the total number of days from due date to final action
     */
    public int getTotalDunningPeriodDays() {
        return step1IntervalDays + step2IntervalDays + step3IntervalDays + finalActionDelayDays;
    }
    
    /**
     * Validates dunning configuration
     */
    public void validate() {
        if (step1IntervalDays <= 0) {
            throw new IllegalStateException("Step 1 interval must be positive");
        }
        if (step2IntervalDays <= 0) {
            throw new IllegalStateException("Step 2 interval must be positive");
        }
        if (step3IntervalDays <= 0) {
            throw new IllegalStateException("Step 3 interval must be positive");
        }
        if (finalActionDelayDays <= 0) {
            throw new IllegalStateException("Final action delay must be positive");
        }
        
        // Ensure reasonable progression
        if (step1IntervalDays >= step2IntervalDays || step2IntervalDays >= step3IntervalDays) {
            throw new IllegalStateException("Dunning intervals should be in ascending order");
        }
    }
}

