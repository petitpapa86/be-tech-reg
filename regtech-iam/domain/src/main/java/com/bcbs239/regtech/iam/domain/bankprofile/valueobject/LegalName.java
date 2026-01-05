package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * Legal Name of the banking institution
 * 
 * Business Rules:
 * - Cannot be null or blank
 * - Maximum length: 255 characters
 */
@Value
public class LegalName {
    String value;
    
    private LegalName(String value) {
        this.value = value;
    }
    
    public static Result<LegalName> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "LEGAL_NAME_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Legal name cannot be null or blank", 
                "validation.legal_name_required"
            ));
        }
        
        String trimmed = value.trim();
        
        if (trimmed.length() > 255) {
            return Result.failure(ErrorDetail.of(
                "LEGAL_NAME_TOO_LONG", 
                ErrorType.VALIDATION_ERROR, 
                "Legal name cannot exceed 255 characters", 
                "validation.legal_name_too_long"
            ));
        }
        
        return Result.success(new LegalName(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
