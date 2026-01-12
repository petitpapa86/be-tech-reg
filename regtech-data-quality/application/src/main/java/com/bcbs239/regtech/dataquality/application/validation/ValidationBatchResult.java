package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.application.validation.timeliness.TimelinessValidator.TimelinessResult;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyValidationResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Result of batch validation containing both the raw validation results,
 * the processed exposure results, optional cross-field consistency checks,
 * and optional timeliness calculation.
 */
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults,
    @Nullable ConsistencyValidationResult consistencyResult,
    @Nullable TimelinessResult timelinessResult
) {
    /**
     * Factory method for backward compatibility - without consistency checks or timeliness.
     */
    public static ValidationBatchResult withoutConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults
    ) {
        return new ValidationBatchResult(results, exposureResults, null, null);
    }
    
    /**
     * Factory method with consistency checks only (no timeliness).
     */
    public static ValidationBatchResult withConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults,
        ConsistencyValidationResult consistencyResult
    ) {
        return new ValidationBatchResult(results, exposureResults, consistencyResult, null);
    }
    
    /**
     * Factory method with both consistency checks and timeliness calculation.
     */
    public static ValidationBatchResult complete(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults,
        ConsistencyValidationResult consistencyResult,
        TimelinessResult timelinessResult
    ) {
        return new ValidationBatchResult(results, exposureResults, consistencyResult, timelinessResult);
    }
    
    /**
     * Check if consistency checks were performed.
     */
    public boolean hasConsistencyChecks() {
        return consistencyResult != null;
    }
    
    /**
     * Check if timeliness calculation was performed.
     */
    public boolean hasTimelinessResult() {
        return timelinessResult != null;
    }
}