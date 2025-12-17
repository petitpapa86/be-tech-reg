package com.bcbs239.regtech.dataquality.application.rulesengine;

import java.util.List;

import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLogDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

/**
 * Contains the results of validation WITHOUT persisting to database.
 * Designed for parallel processing - collect results, then persist in batch.
 */
public record ValidationResultsDto(
    String exposureId,
    List<ValidationError> validationErrors,
    List<RuleViolation> ruleViolations,
    List<RuleExecutionLogDto> executionLogs
) {
    public boolean hasViolations() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    public int getViolationCount() {
        return ruleViolations == null ? 0 : ruleViolations.size();
    }

    public int getExecutionCount() {
        return executionLogs == null ? 0 : executionLogs.size();
    }
}
