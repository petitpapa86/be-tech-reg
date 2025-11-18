package com.bcbs239.regtech.riskcalculation.application.shared;

import lombok.Getter;

/**
 * Retry policy configuration for file download operations.
 * Implements exponential backoff strategy with configurable parameters.
 */
@Getter
public class RetryPolicy {
    
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final long maxBackoffMillis;
    
    private RetryPolicy(int maxAttempts, long initialBackoffMillis, double backoffMultiplier, long maxBackoffMillis) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoffMillis = maxBackoffMillis;
    }
    
    /**
     * Creates a default retry policy with 3 attempts and exponential backoff.
     * Initial backoff: 1000ms, multiplier: 2.0, max backoff: 8000ms
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 1000L, 2.0, 8000L);
    }
    
    /**
     * Creates a custom retry policy with specified parameters.
     */
    public static RetryPolicy custom(int maxAttempts, long initialBackoffMillis, 
                                    double backoffMultiplier, long maxBackoffMillis) {
        return new RetryPolicy(maxAttempts, initialBackoffMillis, backoffMultiplier, maxBackoffMillis);
    }
    
    /**
     * Determines if another retry should be attempted.
     */
    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxAttempts;
    }
    
    /**
     * Calculates the backoff delay for the given attempt number using exponential backoff.
     * Formula: min(initialBackoff * (multiplier ^ (attemptNumber - 1)), maxBackoff)
     */
    public long calculateBackoff(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0L;
        }
        
        long backoff = (long) (initialBackoffMillis * Math.pow(backoffMultiplier, attemptNumber - 1));
        return Math.min(backoff, maxBackoffMillis);
    }
}
