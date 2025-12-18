package com.bcbs239.regtech.dataquality.application.rulesengine;

import java.util.List;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
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
    ValidationExecutionStats stats
) {
    public ValidationResultsDto(String exposureId, List<ValidationError> validationErrors,
                               List<RuleViolation> ruleViolations) {
        this(exposureId, validationErrors, ruleViolations, new ValidationExecutionStats());
    }

}
