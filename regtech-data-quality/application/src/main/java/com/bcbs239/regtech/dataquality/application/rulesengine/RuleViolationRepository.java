package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;

import java.util.List;

/**
 * Port for rule violation persistence.
 */
public interface RuleViolationRepository {

    /**
     * Save a single violation.
     */
    void save(RuleViolation violation);

    /**
     * Save all violations for a batch.
     */
    void saveAllForBatch(String batchId, List<RuleViolation> violations);

    /**
     * Flush pending changes.
     */
    void flush();
}