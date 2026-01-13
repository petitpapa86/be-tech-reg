package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing accuracy error threshold percentage.
 * 
 * <p>Domain Primitive: Ensures accuracy error thresholds are always valid (0.0 to 1.0).
 * 
 * <p>Business Rule: Accuracy error must be between 0% and 100% (0.0 to 1.0 as decimal).
 */
public record AccuracyThreshold(double value) {
    
    /**
     * Creates an accuracy error threshold with validation.
     * 
     * @param value Threshold as decimal (0.0 = 0%, 1.0 = 100%)
     * @return Result containing validated AccuracyThreshold or validation error
     */
    public static Result<AccuracyThreshold> of(double value) {
        if (value < 0.0 || value > 1.0) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_ACCURACY_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Accuracy error threshold must be between 0.0 and 1.0, got: " + value,
                    "validation.accuracy_threshold.out_of_range"
                )
            );
        }
        
        return Result.success(new AccuracyThreshold(value));
    }
    
    /**
     * Gets the threshold as a percentage (0-100).
     * 
     * @return Threshold as percentage
     */
    public double asPercentage() {
        return value * 100.0;
    }
}
