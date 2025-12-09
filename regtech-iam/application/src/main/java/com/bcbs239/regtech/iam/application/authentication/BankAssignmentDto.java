package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.BankId;

/**
 * Bank Assignment DTO
 *
 * Data Transfer Object for bank assignments in authentication responses
 */
public record BankAssignmentDto(
    BankId bankId,
    String bankName,
    String roleName
) {

    public static Result<BankAssignmentDto> from(String bankId, String bankName, String role) {
        Result<BankId> bankIdResult = BankId.fromString(bankId);
        if (bankIdResult.isFailure()) {
            return Result.failure(bankIdResult.getError().get());
        }

        // Validate role name is not null or empty
        if (role == null || role.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_ROLE", ErrorType.VALIDATION_ERROR, "Role cannot be null or empty", "authentication.invalid.role"));
        }

        return Result.success(new BankAssignmentDto(
            bankIdResult.getValue().get(),
            bankName,
            role.trim()
        ));
    }
}

