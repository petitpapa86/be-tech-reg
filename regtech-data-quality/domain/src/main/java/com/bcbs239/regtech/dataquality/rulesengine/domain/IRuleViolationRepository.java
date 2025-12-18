package com.bcbs239.regtech.dataquality.rulesengine.domain;

/**
 * Domain repository interface for rule violations.
 */
public interface IRuleViolationRepository {
    
    /**
     * Saves a rule violation.
     */
    void save(RuleViolation violation);

    /**
     * Flushes pending writes (optional).
     */
    default void flush() {
        // no-op by default
    }
}
