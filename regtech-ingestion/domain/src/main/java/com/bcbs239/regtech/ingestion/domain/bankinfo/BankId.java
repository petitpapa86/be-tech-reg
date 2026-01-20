package com.bcbs239.regtech.ingestion.domain.bankinfo;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * Value object representing a bank identifier.
 */
public record BankId(@JsonValue String value) {
    
    @JsonCreator
    public BankId {
        Objects.requireNonNull(value, "BankId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BankId value cannot be empty");
        }

    }
    
    /**
     * Create a BankId from a string value.
     * Returns Result.success(BankId) when valid, otherwise Result.failure(ErrorDetail)
     */
    public static Result<BankId> of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_REQUIRED",
                ErrorType.SYSTEM_ERROR,
                "Bank ID is required",
                "validation.bank_id_required"
            ));
        }

        // Additional validation could be added here (pattern, length, etc.)
        return Result.success(new BankId(value.trim()));
    }

}
