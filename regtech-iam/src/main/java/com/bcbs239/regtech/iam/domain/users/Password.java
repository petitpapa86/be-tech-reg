package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.util.regex.Pattern;

/**
 * Password Value Object with strong validation requirements
 */
public record Password(String hash) {

    // Password requirements
    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    public static Result<Password> create(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            return Result.failure(ErrorDetail.of("PASSWORD_TOO_SHORT",
                "Password must be at least " + MIN_LENGTH + " characters long",
                "error.password.tooShort"));
        }

        if (!UPPERCASE.matcher(plainPassword).matches()) {
            return Result.failure(ErrorDetail.of("PASSWORD_MISSING_UPPERCASE",
                "Password must contain at least one uppercase letter",
                "error.password.missingUppercase"));
        }

        if (!LOWERCASE.matcher(plainPassword).matches()) {
            return Result.failure(ErrorDetail.of("PASSWORD_MISSING_LOWERCASE",
                "Password must contain at least one lowercase letter",
                "error.password.missingLowercase"));
        }

        if (!DIGIT.matcher(plainPassword).matches()) {
            return Result.failure(ErrorDetail.of("PASSWORD_MISSING_DIGIT",
                "Password must contain at least one number",
                "error.password.missingDigit"));
        }

        if (!SPECIAL_CHAR.matcher(plainPassword).matches()) {
            return Result.failure(ErrorDetail.of("PASSWORD_MISSING_SPECIAL",
                "Password must contain at least one special character",
                "error.password.missingSpecial"));
        }

        // In real implementation, hash the password here with BCrypt
        // For now, we'll just store it (this should be changed in production)
        String hashedPassword = hashPassword(plainPassword);

        return Result.success(new Password(hashedPassword));
    }

    private static String hashPassword(String plainPassword) {
        // TODO: Implement BCrypt hashing with strength 12
        // For now, return a placeholder
        return "HASHED:" + plainPassword;
    }

    public boolean matches(String plainPassword) {
        // TODO: Implement proper password verification
        return ("HASHED:" + plainPassword).equals(hash);
    }
}