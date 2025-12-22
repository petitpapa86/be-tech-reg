package com.bcbs239.regtech.dataquality.rulesengine.domain;

/**
 * Domain repository interface for rule execution logs.
 */
public interface IRuleExecutionLogRepository {
    
    /**
     * Saves a rule execution log.
     */

    /**
     * Flushes pending writes (optional).
     */
    default void flush() {
        // no-op by default
    }
}
