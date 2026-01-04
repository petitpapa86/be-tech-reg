package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

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
            return Result.failure("Bank type cannot be null or blank");
        }
        
        try {
            return Result.success(BankType.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid bank type: " + value);
        }
    }
}
