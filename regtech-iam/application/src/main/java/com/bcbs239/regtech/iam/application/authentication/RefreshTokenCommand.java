package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to refresh access token using refresh token
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 */
public record RefreshTokenCommand(
    String refreshToken
) {
    /**
     * Factory method with validation
     */
    public static Result<RefreshTokenCommand> create(String refreshToken) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        if (refreshToken == null || refreshToken.isBlank()) {
            fieldErrors.add(new FieldError(
                "refreshToken",
                "Refresh token is required",
                "refresh_token.required"
            ));
        }
        
        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        return Result.success(new RefreshTokenCommand(refreshToken));
    }
}
