package com.bcbs239.regtech.iam.domain.users;


import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;

/**
 * Tenant Context Value Object
 *
 * Represents the context of a selected bank tenant for a user session
 */
public record TenantContext(
    BankId bankId,
    String bankName,
    Bcbs239Role role
) {

    public static Result<TenantContext> create(BankId bankId, String bankName, Bcbs239Role role) {
        if (bankId == null) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID",
                "Bank ID cannot be null"));
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_NAME",
                "Bank name cannot be null or empty"));
        }
        if (role == null) {
            return Result.failure(ErrorDetail.of("INVALID_ROLE",
                "Role cannot be null"));
        }
        return Result.success(new TenantContext(bankId, bankName.trim(), role));
    }
}

