package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Banking institution type
 */
@Getter
@RequiredArgsConstructor
public enum BankType {
    COMMERCIAL("Banca Commerciale"),
    INVESTMENT("Banca di Investimento"),
    COOPERATIVE("Banca Cooperativa"),
    POPULAR("Banca Popolare"),
    BCC("Banca di Credito Cooperativo");
    
    private final String displayName;
    
    public static Result<BankType> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "BANK_TYPE_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Bank type cannot be null or blank", 
                "validation.bank_type_required"
            ));
        }
        
        try {
            return Result.success(BankType.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "BANK_TYPE_INVALID", 
                ErrorType.VALIDATION_ERROR, 
                "Invalid bank type: " + value, 
                "validation.bank_type_invalid"
            ));
        }
    }
}
