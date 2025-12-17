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
     * Saves rule violations in batch with an optional batch id.
     *
     * <p>Implementations may use this to populate the persistence-layer {@code batch_id} column.
     * Default implementation delegates to {@link #saveAll(Iterable)}.</p>
     */
    default void saveAllForBatch(String batchId, Iterable<RuleViolation> violations) {
        saveAll(violations);
    }

    /**
     * Flushes pending writes (optional).
     */
    default void flush() {
        // no-op by default
    }
}
