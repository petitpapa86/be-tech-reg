package com.bcbs239.regtech.billing.domain.valueobjects;

import java.time.Duration;

/**
 * Enumeration representing the sequential steps in the dunning process.
 * Each step has a specific timing and escalation level for payment collection.
 */
public enum DunningStep {
    
    /**
     * First reminder sent 1 day after invoice becomes overdue.
     * Gentle reminder with payment instructions.
     */
    STEP_1_REMINDER(Duration.ofDays(1), "First Payment Reminder"),
    
    /**
     * Second reminder sent 7 days after first reminder.
     * More urgent tone with late fee warning.
     */
    STEP_2_REMINDER(Duration.ofDays(7), "Second Payment Reminder"),
    
    /**
     * Final notice sent 14 days after second reminder.
     * Warning of account suspension and collection actions.
     */
    STEP_3_FINAL_NOTICE(Duration.ofDays(14), "Final Payment Notice"),
    
    /**
     * Account suspension executed 7 days after final notice.
     * Service access is suspended until payment is received.
     */
    STEP_4_SUSPENSION(Duration.ofDays(7), "Account Suspension");
    
    private final Duration delayFromPrevious;
    private final String description;
    
    DunningStep(Duration delayFromPrevious, String description) {
        this.delayFromPrevious = delayFromPrevious;
        this.description = description;
    }
    
    /**
     * Get the delay duration from the previous step.
     * 
     * @return Duration to wait before executing this step
     */
    public Duration getDelayFromPrevious() {
        return delayFromPrevious;
    }
    
    /**
     * Get the human-readable description of this step.
     * 
     * @return Description of the dunning step
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the next step in the dunning process.
     * 
     * @return Next DunningStep, or null if this is the final step
     */
    public DunningStep getNextStep() {
        DunningStep[] steps = values();
        int currentIndex = this.ordinal();
        
        if (currentIndex < steps.length - 1) {
            return steps[currentIndex + 1];
        }
        
        return null; // No next step - this is the final step
    }
    
    /**
     * Check if this is the final step in the dunning process.
     * 
     * @return true if this is the final step, false otherwise
     */
    public boolean isFinalStep() {
        return this == STEP_4_SUSPENSION;
    }
    
    /**
     * Get the first step in the dunning process.
     * 
     * @return The initial dunning step
     */
    public static DunningStep getFirstStep() {
        return STEP_1_REMINDER;
    }
}