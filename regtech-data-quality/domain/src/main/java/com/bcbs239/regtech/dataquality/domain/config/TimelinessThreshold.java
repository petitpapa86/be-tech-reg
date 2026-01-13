package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing timeliness threshold in days.
 * 
 * <p>Domain Primitive: Ensures timeliness thresholds are always valid (positive integer).
 * 
 * <p>Business Rule: Timeliness must be a positive number of days.
 */
public record TimelinessThreshold(int value) {
    
    /**
     * Creates a timeliness threshold with validation.
     * 
     * @param value Number of days (must be positive)
     * @return Result containing validated TimelinessThreshold or validation error
     */
    public static Result<TimelinessThreshold> of(int value) {
        if (value <= 0) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_TIMELINESS_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Timeliness threshold must be positive, got: " + value,
                    "validation.timeliness_threshold.not_positive"
                )
            );
        }
        
        return Result.success(new TimelinessThreshold(value));
    }
}
