package com.bcbs239.regtech.iam.application.authentication;

/**
 * LogoutResponse - Response for logout operation
 * 
 * Requirements: 4.4
 */
public record LogoutResponse(
    String message,
    String messageKey
) {
    /**
     * Factory method to create a successful logout response
     */
    public static LogoutResponse success() {
        return new LogoutResponse(
            "Logged out successfully",
            "logout.success"
        );
    }
}
