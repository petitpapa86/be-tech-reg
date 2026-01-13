package com.bcbs239.regtech.dataquality.domain.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

/**
 * Value object representing a bank identifier.
 * 
 * <p><b>Domain Layer:</b> Pure business concept with validation.
 */
public record BankId(String value) {
    
    /**
     * Creates a bank ID with validation.
     * 
     * @param value The bank identifier string
     * @return Result with validated BankId or error
     */
    public static Result<BankId> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(
                ErrorDetail.of(
                    "BANK_ID_REQUIRED",
                    ErrorType.VALIDATION_ERROR,
                    "Bank ID is required",
                    "validation.bank_id.required"
                )
            );
        }
        return Result.success(new BankId(value));
    }
}
