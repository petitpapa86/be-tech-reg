package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.util.ArrayList;
import java.util.List;

/**
 * LogoutCommand - Command for user logout
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
public record LogoutCommand(
    UserId userId,
    String refreshToken
) {
    /**
     * Factory method to create and validate a LogoutCommand
     */
    public static Result<LogoutCommand> create(String userId, String refreshToken) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        // Validate userId
        if (userId == null || userId.isBlank()) {
            fieldErrors.add(new FieldError(
                "userId",
                "User ID is required",
                "logout.user_id.required"
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
                "logout.user_id.invalid"
            ));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        return Result.success(new LogoutCommand(
            userIdValue,
            refreshToken
        ));
    }
}
