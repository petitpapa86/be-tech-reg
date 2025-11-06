package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.domain.users.Bcbs239Role;

/**
 * Bank Assignment DTO
 *
 * Data Transfer Object for bank assignments in authentication responses
 */
public record BankAssignmentDto(
    BankId bankId,
    String bankName,
    Bcbs239Role role
) {

    public static Result<BankAssignmentDto> from(String bankId, String bankName, String role) {
        Result<BankId> bankIdResult = BankId.fromString(bankId);
        if (bankIdResult.isFailure()) {
            return Result.failure(bankIdResult.getError().get());
        }

        try {
            Bcbs239Role bcbsRole = Bcbs239Role.valueOf(role.toUpperCase());
            return Result.success(new BankAssignmentDto(
                bankIdResult.getValue().get(),
                bankName,
                bcbsRole
            ));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("INVALID_ROLE", ErrorType.VALIDATION_ERROR, "Invalid role: " + role, "authentication.invalid.role"));
        }
    }
}

