package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

/**
 * Port for exposure validation functionality.
 * Defines the contract for validating exposure records.
 */
public interface ExposureRuleValidator {

    /**
     * Validates a single exposure record without persisting results.
     *
     * @param exposure The exposure record to validate
     * @return ValidationResults containing errors, violations, logs, and stats
     */
    ValidationResults validateNoPersist(ExposureRecord exposure);
}