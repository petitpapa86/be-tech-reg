package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

import java.util.List;

/**
 * Contains the results of validation including statistics.
 */
public record ValidationResults(
    String exposureId,
    List<ValidationError> validationErrors,
    List<RuleViolation> ruleViolations,
    ValidationExecutionStats stats
) {
    public int getViolationCount() {
        return ruleViolations == null ? 0 : ruleViolations.size();
    }

}