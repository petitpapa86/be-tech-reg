package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

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
            return Result.failure("LEI code cannot be null");
        }
        
        String normalized = value.trim().toUpperCase();
        
        if (normalized.isEmpty()) {
            return Result.failure("LEI code cannot be empty");
        }
        
        if (!normalized.matches("[A-Z0-9]{20}")) {
            return Result.failure("LEI code must be exactly 20 alphanumeric characters");
        }
        
        return Result.success(new LeiCode(normalized));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
