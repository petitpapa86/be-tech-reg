package com.bcbs239.regtech.billing.domain.valueobjects;

/**
 * Enumeration representing the possible statuses of a dunning case.
 * Tracks the lifecycle of dunning processes from creation to resolution.
 */
public enum DunningCaseStatus {
    
    /**
     * Dunning case has been created and is actively being processed.
     * Steps are being executed according to the dunning schedule.
     */
    IN_PROGRESS,
    
    /**
     * Dunning case has been resolved successfully.
     * Payment was received or account was brought current.
     */
    RESOLVED,
    
    /**
     * Dunning case was cancelled before completion.
     * May occur due to account cancellation or manual intervention.
     */
    CANCELLED,
    
    /**
     * Dunning case has failed after all steps were exhausted.
     * Account may be suspended or sent to collections.
     */
    FAILED
}