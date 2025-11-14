package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.security.Authentication;

/**
 * Domain service for user authentication.
 * Handles token validation and user authentication.
 */
public interface AuthenticationService {

    /**
     * Authenticate a user with the provided token.
     * Returns the authentication information if the token is valid.
     *
     * @param token The authentication token (JWT, API key, etc.)
     * @return Authentication object if token is valid, null otherwise
     * @throws AuthenticationException if authentication fails
     */
    Authentication authenticate(String token) throws AuthenticationException;

    /**
     * Validate if a token is valid without extracting full authentication info.
     *
     * @param token The authentication token to validate
     * @return true if token is valid, false otherwise
     */
    boolean isValidToken(String token);

    /**
     * Extract user ID from token without full authentication.
     *
     * @param token The authentication token
     * @return user ID if token is valid, null otherwise
     */
    String extractUserId(String token);

    /**
     * Exception thrown when authentication fails.
     */
    class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}