package com.bcbs239.regtech.iam.application.authentication;

/**
 * Authentication Command
 *
 * Represents a user authentication request with email and password
 */
public record AuthenticationCommand(
    String email,
    String password
) {

    /**
     * Validates the authentication command
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }
}

