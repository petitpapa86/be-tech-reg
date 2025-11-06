package com.bcbs239.regtech.iam.domain.users;


import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;

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
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR, "Bank ID is required", "validation.invalid.bank.id"));
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_NAME", ErrorType.VALIDATION_ERROR, "Bank name is required and cannot be empty", "validation.invalid.bank.name"));
        }
        if (role == null) {
            return Result.failure(ErrorDetail.of("INVALID_ROLE", ErrorType.VALIDATION_ERROR, "Role is required", "validation.invalid.role"));
        }
        return Result.success(new TenantContext(bankId, bankName.trim(), role));
    }
}



