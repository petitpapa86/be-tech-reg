package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * LoginCommand - Command for user login with email and password
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
public record LoginCommand(
    String email,
    String password,
    Maybe<String> ipAddress
) {
    /**
     * Factory method to create and validate a LoginCommand
     */
    public static Result<LoginCommand> create(
        String email,
        String password,
        String ipAddress
    ) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        // Validate email
        if (email == null || email.isBlank()) {
            fieldErrors.add(new FieldError(
                "email",
                "Email is required",
                "login.email.required"
            ));
        }
        
        // Validate password
        if (password == null || password.isBlank()) {
            fieldErrors.add(new FieldError(
                "password",
                "Password is required",
                "login.password.required"
            ));
        }
        
        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        return Result.success(new LoginCommand(
            email,
            password,
            ipAddress != null && !ipAddress.isBlank() ? Maybe.some(ipAddress) : Maybe.none()
        ));
    }
}
