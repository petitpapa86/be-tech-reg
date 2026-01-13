package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing timeliness threshold in days.
 * 
 * <p>Must be a positive integer.
 * 
 * <p><b>Domain Layer:</b> Pure business concept with validation.
 */
public record TimelinessDays(int value) {
    
    /**
     * Creates a timeliness threshold with validation.
     * 
     * @param value The number of days (must be positive)
     * @return Result with validated TimelinessDays or error
     */
    public static Result<TimelinessDays> of(int value) {
        if (value < 0) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_TIMELINESS_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Timeliness threshold must be positive",
                    "validation.timeliness.positive"
                )
            );
        }
        return Result.success(new TimelinessDays(value));
    }
}
