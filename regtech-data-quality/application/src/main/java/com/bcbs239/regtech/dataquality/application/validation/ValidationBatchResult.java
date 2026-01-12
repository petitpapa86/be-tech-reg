package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.application.validation.timeliness.TimelinessValidator.TimelinessResult;
import com.bcbs239.regtech.dataquality.application.validation.uniqueness.UniquenessValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.consistency.ConsistencyValidationResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Result of batch validation containing both the raw validation results,
 * the processed exposure results, optional cross-field consistency checks,
 * optional timeliness calculation, and optional uniqueness validation.
 */
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults,
    @Nullable ConsistencyValidationResult consistencyResult,
    @Nullable TimelinessResult timelinessResult,
    @Nullable UniquenessValidationResult uniquenessResult
) {
    /**
     * Factory method for backward compatibility - without consistency checks or timeliness.
     */
    public static ValidationBatchResult withoutConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults
    ) {
        return new ValidationBatchResult(results, exposureResults, null, null, null);
    }
    
    /**
     * Factory method with consistency checks only (no timeliness or uniqueness).
     */
    public static ValidationBatchResult withConsistencyChecks(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults,
        ConsistencyValidationResult consistencyResult
    ) {
        return new ValidationBatchResult(results, exposureResults, consistencyResult, null, null);
    }
    
    /**
     * Factory method with consistency checks, timeliness, and uniqueness validation.
     */
    public static ValidationBatchResult complete(
        List<ValidationResults> results,
        Map<String, ExposureValidationResult> exposureResults,
        ConsistencyValidationResult consistencyResult,
        TimelinessResult timelinessResult,
        UniquenessValidationResult uniquenessResult
    ) {
        return new ValidationBatchResult(results, exposureResults, consistencyResult, timelinessResult, uniquenessResult);
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
    
    /**
     * Check if uniqueness validation was performed.
     */
    public boolean hasUniquenessResult() {
        return uniquenessResult != null;
    }
}