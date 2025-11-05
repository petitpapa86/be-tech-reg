package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.application.shared.Result;

import java.util.regex.Pattern;

/**
 * Email Value Object with validation
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static Result<Email> create(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EMAIL_REQUIRED", "Email is required", "error.email.required"));
        }

        String trimmedEmail = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return Result.failure(ErrorDetail.of("EMAIL_INVALID", "Invalid email format", "error.email.invalid"));
        }

        return Result.success(new Email(trimmedEmail));
    }

    public String getValue() {
        return value;
    }
}