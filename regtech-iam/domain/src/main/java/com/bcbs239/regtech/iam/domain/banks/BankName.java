package com.bcbs239.regtech.iam.domain.banks;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * BankName Value Object
 * Represents a validated bank name
 * Requirements: 13.1, 13.2
 */
public record BankName(String value) {
    
    public static Result<BankName> create(String value) {
        List<FieldError> errors = new ArrayList<>();
        
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(
                "name",
                "Bank name is required",
                "bank.name.required"
            ));
        } else {
            String trimmed = value.trim();
            
            if (trimmed.length() < 2) {
                errors.add(new FieldError(
                    "name",
                    "Bank name must be at least 2 characters",
                    "bank.name.too_short"
                ));
            }
            
            if (trimmed.length() > 200) {
                errors.add(new FieldError(
                    "name",
                    "Bank name must not exceed 200 characters",
                    "bank.name.too_long"
                ));
            }
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(errors));
        }
        
        return Result.success(new BankName(value.trim()));
    }
}
