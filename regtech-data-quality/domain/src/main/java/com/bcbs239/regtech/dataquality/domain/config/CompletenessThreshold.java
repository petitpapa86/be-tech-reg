package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing completeness threshold percentage.
 * 
 * <p>Domain Primitive: Ensures completeness thresholds are always valid (0.0 to 1.0).
 * 
 * <p>Business Rule: Completeness must be between 0% and 100% (0.0 to 1.0 as decimal).
 */
public record CompletenessThreshold(double value) {
    
    /**
     * Creates a completeness threshold with validation.
     * 
     * @param value Threshold as decimal (0.0 = 0%, 1.0 = 100%)
     * @return Result containing validated CompletenessThreshold or validation error
     */
    public static Result<CompletenessThreshold> of(double value) {
        if (value < 0.0 || value > 1.0) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_COMPLETENESS_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Completeness threshold must be between 0.0 and 1.0, got: " + value,
                    "validation.completeness_threshold.out_of_range"
                )
            );
        }
        
        return Result.success(new CompletenessThreshold(value));
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
