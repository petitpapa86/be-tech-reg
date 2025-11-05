package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.application.shared.Result;

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
                "Bank ID cannot be null", "error.tenantContext.invalidBankId"));
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_NAME",
                "Bank name cannot be null or empty", "error.tenantContext.invalidBankName"));
        }
        if (role == null) {
            return Result.failure(ErrorDetail.of("INVALID_ROLE",
                "Role cannot be null", "error.tenantContext.invalidRole"));
        }
        return Result.success(new TenantContext(bankId, bankName.trim(), role));
    }
}