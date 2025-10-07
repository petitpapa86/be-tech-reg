package com.bcbs239.regtech.core.saga;

import java.time.Duration;

/**
 * Business timeout service for managing time-based saga operations.
 * Handles timeouts for business processes like payment deadlines, approval windows, etc.
 */
public interface BusinessTimeoutService {

    /**
     * Schedules a timeout for a saga
     */
    void scheduleTimeout(String sagaId, String timeoutType, Duration duration, Runnable callback);

    /**
     * Cancels a scheduled timeout
     */
    void cancelTimeout(String sagaId, String timeoutType);

    /**
     * Extends an existing timeout
     */
    void extendTimeout(String sagaId, String timeoutType, Duration additionalDuration);

    /**
     * Checks if a timeout is active for a saga
     */
    boolean isTimeoutActive(String sagaId, String timeoutType);

    /**
     * Gets the remaining time for a timeout
     */
    Duration getRemainingTime(String sagaId, String timeoutType);

    /**
     * Handles timeout expiration (called by the timeout scheduler)
     */
    void handleTimeout(String sagaId, String timeoutType);
}