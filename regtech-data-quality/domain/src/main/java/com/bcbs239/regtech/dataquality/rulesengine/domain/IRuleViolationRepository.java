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
     * Saves rule violations in batch.
     * Implementations backed by JPA should prefer bulk inserts.
     */
    default void saveAll(Iterable<RuleViolation> violations) {
        if (violations == null) {
            return;
        }
        for (RuleViolation violation : violations) {
            save(violation);
        }
    }

    /**
     * Flushes pending writes (optional).
     */
    default void flush() {
        // no-op by default
    }
}
