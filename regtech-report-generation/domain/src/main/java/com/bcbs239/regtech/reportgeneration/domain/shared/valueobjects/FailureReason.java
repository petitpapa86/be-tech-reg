package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.time.Instant;

/**
 * Failure reason value object with categorization
 * Captures detailed information about report generation failures
 */
public record FailureReason(
    @NonNull FailureCategory category,
    @NonNull String message,
    @NonNull Instant occurredAt
) {
    
    /**
     * Compact constructor with validation
     */
    public FailureReason {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Failure message cannot be null or blank");
        }
    }
    
    /**
     * Create a failure reason with current timestamp
     */
    public static FailureReason of(FailureCategory category, String message) {
        return new FailureReason(category, message, Instant.now());
    }
    
    /**
     * Create a data validation failure
     */
    public static FailureReason dataValidation(String message) {
        return of(FailureCategory.DATA_VALIDATION, message);
    }
    
    /**
     * Create a file storage failure
     */
    public static FailureReason fileStorage(String message) {
        return of(FailureCategory.FILE_STORAGE, message);
    }
    
    /**
     * Create a template rendering failure
     */
    public static FailureReason templateRendering(String message) {
        return of(FailureCategory.TEMPLATE_RENDERING, message);
    }
    
    /**
     * Create an XBRL validation failure
     */
    public static FailureReason xbrlValidation(String message) {
        return of(FailureCategory.XBRL_VALIDATION, message);
    }
    
    /**
     * Create a database failure
     */
    public static FailureReason database(String message) {
        return of(FailureCategory.DATABASE, message);
    }
    
    /**
     * Create a network failure
     */
    public static FailureReason network(String message) {
        return of(FailureCategory.NETWORK, message);
    }
    
    /**
     * Create an unknown failure
     */
    public static FailureReason unknown(String message) {
        return of(FailureCategory.UNKNOWN, message);
    }
    
    /**
     * Check if this is a transient failure that can be retried
     */
    public boolean isTransient() {
        return category.isTransient();
    }
    
    /**
     * Check if this is a permanent failure that should not be retried
     */
    public boolean isPermanent() {
        return !isTransient();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (at %s)", category, message, occurredAt);
    }
    
    /**
     * Categories of failures for report generation
     */
    public enum FailureCategory {
        DATA_VALIDATION(false),
        FILE_STORAGE(true),
        TEMPLATE_RENDERING(false),
        XBRL_VALIDATION(false),
        DATABASE(true),
        NETWORK(true),
        UNKNOWN(false);
        
        private final boolean transient_;
        
        FailureCategory(boolean transient_) {
            this.transient_ = transient_;
        }
        
        public boolean isTransient() {
            return transient_;
        }
    }
}
