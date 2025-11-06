package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

import java.util.UUID;

/**
 * Bank ID Value Object
 */
public record BankId(String value) {

    public static BankId generate() {
        return new BankId(UUID.randomUUID().toString());
    }

    public static Result<BankId> fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR, "Bank ID cannot be null or empty", "validation.invalid_bank_id"));
        }
        return Result.success(new BankId(value.trim()));
    }

    public String getValue() {
        return value;
    }
}



