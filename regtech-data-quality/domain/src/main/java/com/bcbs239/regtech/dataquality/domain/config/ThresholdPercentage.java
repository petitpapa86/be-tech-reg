package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing a threshold percentage (0-100).
 * 
 * <p>Used for quality thresholds like completeness, accuracy, and consistency.
 * 
 * <p><b>Domain Layer:</b> Pure business concept with validation.
 */
public record ThresholdPercentage(double value) {
    
    /**
     * Creates a threshold percentage with validation.
     * 
     * @param value The percentage value (0-100)
     * @param fieldName Name of the field for error messages
     * @return Result with validated ThresholdPercentage or error
     */
    public static Result<ThresholdPercentage> of(double value, String fieldName) {
        if (value < 0 || value > 100) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_" + fieldName.toUpperCase() + "_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    fieldName + " threshold must be between 0 and 100",
                    "validation." + fieldName.toLowerCase() + ".range"
                )
            );
        }
        return Result.success(new ThresholdPercentage(value));
    }
    
    /**
     * Creates a threshold percentage from integer value.
     */
    public static Result<ThresholdPercentage> of(int value, String fieldName) {
        return of((double) value, fieldName);
    }
}
