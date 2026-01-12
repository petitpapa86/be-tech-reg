package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyValidationResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Result of batch validation containing both the raw validation results,
 * the processed exposure results, and optional cross-field consistency checks.
 */
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults,
    @Nullable ConsistencyValidationResult consistencyResult
) {
    /**
     * Factory method for backward compatibility - without consistency checks.
     */
    public static ValidationBatchResult withoutConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults
    ) {
        return new ValidationBatchResult(results, exposureResults, null);
    }
    
    /**
     * Factory method with consistency checks.
     */
    public static ValidationBatchResult withConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults,
        ConsistencyValidationResult consistencyResult
    ) {
        return new ValidationBatchResult(results, exposureResults, consistencyResult);
    }
    
    /**
     * Check if consistency checks were performed.
     */
    public boolean hasConsistencyChecks() {
        return consistencyResult != null;
    }
}