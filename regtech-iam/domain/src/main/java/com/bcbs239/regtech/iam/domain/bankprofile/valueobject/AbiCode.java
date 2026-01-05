package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * ABI Code - Associazione Bancaria Italiana
 * 
 * Business Rules:
 * - Exactly 5 digits
 * - Assigned by Banca d'Italia
 * 
 * Smart constructor returns Result to handle validation errors.
 */
@Value
public class AbiCode {
    String value;
    
    private AbiCode(String value) {
        this.value = value;
    }
    
    /**
     * Smart constructor - returns Result&lt;AbiCode&gt;
     * @param value the ABI code string
     * @return Result containing AbiCode or validation error
     */
    public static Result<AbiCode> of(String value) {
        if (value == null) {
            return Result.failure(ErrorDetail.of(
                "ABI_CODE_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "ABI code cannot be null", 
                "validation.abi_code_required"
            ));
        }
        
        String trimmed = value.trim();
        
        if (trimmed.isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "ABI_CODE_EMPTY", 
                ErrorType.VALIDATION_ERROR, 
                "ABI code cannot be empty", 
                "validation.abi_code_empty"
            ));
        }
        
        if (!trimmed.matches("\\d{5}")) {
            return Result.failure(ErrorDetail.of(
                "ABI_CODE_INVALID_FORMAT", 
                ErrorType.VALIDATION_ERROR, 
                "ABI code must be exactly 5 digits", 
                "validation.abi_code_invalid_format"
            ));
        }
        
        return Result.success(new AbiCode(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
