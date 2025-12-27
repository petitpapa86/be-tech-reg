package com.bcbs239.regtech.dataquality.domain.report;

import lombok.Getter;

/**
 * Enumeration representing the status of a quality validation process.
 */
@Getter
public enum QualityStatus {
    
    /**
     * Quality validation has been initiated but not yet started.
     */
    PENDING("Pending validation"),
    
    /**
     * Quality validation is currently in progress.
     */
    IN_PROGRESS("Validation in progress"),
    
    /**
     * Quality validation completed successfully.
     */
    COMPLETED("Validation completed"),
    
    /**
     * Quality validation failed due to an error.
     */
    FAILED("Validation failed");
    
    private final String displayName;
    
    QualityStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Check if the status indicates validation is complete (either success or failure).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
    
    /**
     * Check if the status indicates validation is still active.
     */
    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS;
    }
    
    /**
     * Check if the status indicates successful completion.
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}

