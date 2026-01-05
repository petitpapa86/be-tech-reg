package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Email recipient for report distribution
 * 
 * Smart constructor returns Result for required fields,
 * Maybe for optional fields
 */
@Value
public class EmailRecipient {
    String email;
    
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private EmailRecipient(String email) {
        this.email = email;
    }
    
    /**
     * Smart constructor for required email (primary recipient)
     */
    public static Result<EmailRecipient> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EMAIL",
                ErrorType.VALIDATION_ERROR,
                "Email address cannot be null or blank",
                "report.email.required"
            ));
        }
        
        String trimmed = value.trim();
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EMAIL",
                ErrorType.VALIDATION_ERROR,
                "Invalid email format: " + value,
                "report.email.invalidFormat"
            ));
        }
        
        return Result.success(new EmailRecipient(trimmed));
    }
    
    /**
     * Smart constructor for optional email (CC recipient)
     */
    public static Maybe<EmailRecipient> ofOptional(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.none();
        }
        
        String trimmed = value.trim();
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Maybe.none();
        }
        
        return Maybe.some(new EmailRecipient(trimmed));
    }
    
    @Override
    public String toString() {
        return email;
    }
}
