package com.bcbs239.regtech.core.domain.saga;

/**
 * Domain interface for timeout scheduling operations.
 * Provides abstraction over timeout scheduling infrastructure for clean architecture compliance.
 */
public interface TimeoutScheduler {

    /**
     * Schedule a task to run after a delay.
     * @param key unique identifier for the scheduled task
     * @param delayMillis delay in milliseconds
     * @param task the task to run
     */
    void schedule(String key, long delayMillis, Runnable task);

    /**
     * Cancel a scheduled task.
     * @param key the unique identifier of the task to cancel
     */
    void cancel(String key);
}

