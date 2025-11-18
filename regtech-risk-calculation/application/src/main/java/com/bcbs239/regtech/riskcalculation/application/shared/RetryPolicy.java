package com.bcbs239.regtech.riskcalculation.application.shared;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for retry policies used in file processing operations.
 * Defines retry behavior including maximum attempts and backoff strategy.
 */
@Getter
@Builder
public class RetryPolicy {
    
    /**
     * Maximum number of retry attempts (default: 3)
     */
    @Builder.Default
    private final int maxAttempts = 3;
    
    /**
     * Initial backoff delay in milliseconds (default: 1000ms = 1s)
     */
    @Builder.Default
    private final long initialBackoffMillis = 1000L;
    
    /**
     * Backoff multiplier for exponential backoff (default: 2.0)
     */
    @Builder.Default
    private final double backoffMultiplier = 2.0;
    
    /**
     * Maximum backoff delay in milliseconds (default: 10000ms = 10s)
     */
    @Builder.Default
    private final long maxBackoffMillis = 10000L;
    
    /**
     * Creates a default retry policy with standard settings.
     * - 3 maximum attempts
     * - 1s initial backoff
     * - 2x exponential multiplier
     * - 10s maximum backoff
     * 
     * @return Default retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }
    
    /**
     * Calculates the backoff delay for a given attempt number.
     * Uses exponential backoff: initialBackoff * (multiplier ^ (attemptNumber - 1))
     * Capped at maxBackoffMillis.
     * 
     * @param attemptNumber The current attempt number (1-based)
     * @return Backoff delay in milliseconds
     */
    public long calculateBackoff(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0L;
        }
        
        double backoff = initialBackoffMillis * Math.pow(backoffMultiplier, attemptNumber - 1);
        return Math.min((long) backoff, maxBackoffMillis);
    }
    
    /**
     * Validates if another retry attempt should be made.
     * 
     * @param currentAttempt The current attempt number (1-based)
     * @return true if more retries are allowed, false otherwise
     */
    public boolean shouldRetry(int currentAttempt) {
        return currentAttempt < maxAttempts;
    }
}
