package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * LEI Code - Legal Entity Identifier
 * ISO 17442 standard
 * 
 * Business Rules:
 * - Exactly 20 alphanumeric characters
 * - Uppercase letters and digits only
 */
@Value
public class LeiCode {
    String value;
    
    private LeiCode(String value) {
        this.value = value;
    }
    
    public static Result<LeiCode> of(String value) {
        if (value == null) {
            return Result.failure(ErrorDetail.of(
                "LEI_CODE_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "LEI code cannot be null", 
                "validation.lei_code_required"
            ));
        }
        
        String normalized = value.trim().toUpperCase();
        
        if (normalized.isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "LEI_CODE_EMPTY", 
                ErrorType.VALIDATION_ERROR, 
                "LEI code cannot be empty", 
                "validation.lei_code_empty"
            ));
        }
        
        if (!normalized.matches("[A-Z0-9]{20}")) {
            return Result.failure(ErrorDetail.of(
                "LEI_CODE_INVALID_FORMAT", 
                ErrorType.VALIDATION_ERROR, 
                "LEI code must be exactly 20 alphanumeric characters", 
                "validation.lei_code_invalid_format"
            ));
        }
        
        return Result.success(new LeiCode(normalized));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
