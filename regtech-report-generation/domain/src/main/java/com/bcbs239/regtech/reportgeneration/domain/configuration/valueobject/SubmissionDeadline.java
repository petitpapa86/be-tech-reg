package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * Number of days available for report submission after period end
 * 
 * Business Rule: Must be between 1 and 30 days
 */
@Value
public class SubmissionDeadline {
    int days;
    
    private SubmissionDeadline(int days) {
        this.days = days;
    }
    
    public static Result<SubmissionDeadline> of(int value) {
        if (value < 1 || value > 30) {
            return Result.failure(ErrorDetail.of(
                "INVALID_SUBMISSION_DEADLINE",
                ErrorType.VALIDATION_ERROR,
                "Submission deadline must be between 1 and 30 days, got: " + value,
                "report.submissionDeadline.outOfRange"
            ));
        }
        
        return Result.success(new SubmissionDeadline(value));
    }
    
    /**
     * Domain behavior: Is this a tight deadline?
     */
    public boolean isTight() {
        return days <= 10;
    }
    
    @Override
    public String toString() {
        return days + " giorni";
    }
}
