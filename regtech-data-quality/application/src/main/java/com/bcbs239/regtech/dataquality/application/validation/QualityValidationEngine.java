package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;

import java.util.List;

/**
 * Service interface for validating exposure data across all quality dimensions.
 * Implements the six-dimensional quality validation using the Specification pattern.
 */
public interface QualityValidationEngine {
    
    /**
     * Validates a list of exposures across all six quality dimensions.
     * 
     * @param exposures List of exposure records to validate
     * @return ValidationResult containing all validation errors and dimension scores
     */
    Result<ValidationResult> validateExposures(List<ExposureRecord> exposures);
    
    /**
     * Validates a single exposure record across all dimensions.
     * 
     * @param exposure Single exposure record to validate
     * @return ValidationResult for the single exposure
     */
    Result<ValidationResult> validateSingleExposure(ExposureRecord exposure);
    
    /**
     * Validates batch-level constraints (e.g., uniqueness across the batch).
     * 
     * @param exposures List of exposure records for batch validation
     * @return ValidationResult containing batch-level validation errors
     */
    Result<ValidationResult> validateBatchLevel(List<ExposureRecord> exposures);
}

