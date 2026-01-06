package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;

import java.util.List;

/**
 * Port for rule violation persistence.
 */
public interface RuleViolationRepository {

    /**
     * Saves a single rule violation.
     *
     * @param violation the violation to save
     */
    void save(RuleViolation violation);

    /**
     * Batch inserts multiple violations efficiently.
     *
     * @param batchId the batch identifier for grouping violations
     * @param violations the list of violations to insert
     */
    void insertViolations(String batchId, List<RuleViolation> violations);
}