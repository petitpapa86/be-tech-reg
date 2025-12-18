package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;

import java.util.List;
import java.util.Map;

/**
 * Result of batch validation containing both the raw validation results and the processed exposure results.
 */
public record ValidationBatchResult(
    List<ValidationResults> results,
    Map<String, ExposureValidationResult> exposureResults
) {}