package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

/**
 * Port for rule execution functionality.
 * Defines the contract for executing business rules against exposure data.
 */
public interface RuleExecutionPort {

    /**
     * Executes a single business rule against the given context.
     *
     * @param rule The business rule to execute
     * @param context The rule execution context
     * @param exposure The exposure record
     * @param errors List to collect validation errors
     * @param violations List to collect rule violations
     * @param stats Statistics collector
     */
    void execute(
        com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRuleDto rule,
        com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext context,
        com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord exposure,
        java.util.List<ValidationError> errors,
        java.util.List<RuleViolation> violations,
        ValidationExecutionStats stats
    );

    /**
     * Optional hook for batch validation: pre-load exemption data in bulk.
     * Default implementation is a no-op.
     */
    default void preloadExemptionsForBatch(
        String entityType,
        java.util.List<String> entityIds,
        java.time.LocalDate currentDate
    ) {
        // no-op
    }

    /**
     * Optional hook for batch validation: clear any in-memory caches.
     * Default implementation is a no-op.
     */
    default void clearExemptionCache() {
        // no-op
    }
}