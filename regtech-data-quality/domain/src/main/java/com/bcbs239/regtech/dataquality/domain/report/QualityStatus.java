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

    /**
     * Safely parses a string to QualityStatus.
     * Returns null if the input is null, blank, "all", or invalid.
     * 
     * @param value The string value to parse
     * @return QualityStatus or null
     */
    public static QualityStatus from(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return QualityStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the color code associated with this status.
     */
    public String getColor() {
        return switch (this) {
            case COMPLETED -> "green";
            case IN_PROGRESS, PENDING -> "blue";
            case FAILED -> "red";
        };
    }

    /**
     * Get the icon symbol associated with this status.
     */
    public String getIcon() {
        return switch (this) {
            case COMPLETED -> "✓";
            case IN_PROGRESS, PENDING -> "⟳";
            case FAILED -> "✗";
        };
    }
}

