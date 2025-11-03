package com.bcbs239.regtech.billing.infrastructure.configuration;

/**
 * Type-safe configuration for outbox pattern settings.
 * Defines intervals and retry behavior for reliable event delivery.
 */
public record OutboxConfiguration(
    boolean enabled,
    long processingIntervalMs,
    long retryIntervalMs,
    int maxRetries,
    long cleanupIntervalMs,
    int cleanupRetentionDays
) {
    
    /**
     * Checks if outbox pattern is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the processing interval in milliseconds
     */
    public long getProcessingIntervalMs() {
        return processingIntervalMs;
    }
    
    /**
     * Gets the retry interval in milliseconds
     */
    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }
    
    /**
     * Gets the maximum number of retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * Gets the cleanup interval in milliseconds
     */
    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }
    
    /**
     * Gets the retention period for cleanup in days
     */
    public int getCleanupRetentionDays() {
        return cleanupRetentionDays;
    }
    
    /**
     * Validates outbox configuration
     */
    public void validate() {
        if (enabled) {
            if (processingIntervalMs <= 0) {
                throw new IllegalStateException("Processing interval must be positive when outbox is enabled");
            }
            if (retryIntervalMs <= 0) {
                throw new IllegalStateException("Retry interval must be positive when outbox is enabled");
            }
            if (maxRetries < 0) {
                throw new IllegalStateException("Max retries cannot be negative");
            }
            if (cleanupIntervalMs <= 0) {
                throw new IllegalStateException("Cleanup interval must be positive when outbox is enabled");
            }
            if (cleanupRetentionDays <= 0) {
                throw new IllegalStateException("Cleanup retention days must be positive when outbox is enabled");
            }
            
            // Reasonable business rules
            if (processingIntervalMs < 1000) {
                throw new IllegalStateException("Processing interval should be at least 1 second");
            }
            if (retryIntervalMs < processingIntervalMs) {
                throw new IllegalStateException("Retry interval should be at least as long as processing interval");
            }
            if (maxRetries > 10) {
                throw new IllegalStateException("Max retries should not exceed 10 to avoid infinite loops");
            }
        }
    }
}
