package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectBankCommand - Command for selecting a bank context after login
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
public record SelectBankCommand(
    UserId userId,
    BankId bankId,
    String refreshToken
) {
    /**
     * Creates and validates a SelectBankCommand
     */
    public static Result<SelectBankCommand> create(
        String userId,
        String bankId,
        String refreshToken
    ) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        // Validate userId (Requirement 3.1)
        if (userId == null || userId.isBlank()) {
            fieldErrors.add(new FieldError(
                "userId",
                "User ID is required",
                "select_bank.user_id.required"
            ));
        }
        
        // Validate bankId (Requirement 3.1)
        if (bankId == null || bankId.isBlank()) {
            fieldErrors.add(new FieldError(
                "bankId",
                "Bank ID is required",
                "select_bank.bank_id.required"
            ));
        }
        
        // Validate refreshToken (Requirement 3.1)
        if (refreshToken == null || refreshToken.isBlank()) {
            fieldErrors.add(new FieldError(
                "refreshToken",
                "Refresh token is required",
                "select_bank.refresh_token.required"
            ));
        }
        
        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        // Parse userId
        UserId userIdValue;
        try {
            userIdValue = UserId.fromString(userId);
        } catch (IllegalArgumentException e) {
            fieldErrors.add(new FieldError(
                "userId",
                "Invalid user ID format",
                "select_bank.user_id.invalid_format"
            ));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        // Parse bankId
        Result<BankId> bankIdResult = BankId.fromString(bankId);
        if (bankIdResult.isFailure()) {
            return Result.failure(bankIdResult.getError().get());
        }
        
        return Result.success(new SelectBankCommand(
            userIdValue,
            bankIdResult.getValue().get(),
            refreshToken
        ));
    }
}
