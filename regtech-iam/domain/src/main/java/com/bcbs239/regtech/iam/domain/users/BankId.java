package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.application.shared.Result;

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
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID",
                "Bank ID cannot be null or empty", "error.bankId.invalid"));
        }
        return Result.success(new BankId(value.trim()));
    }

    public String getValue() {
        return value;
    }
}