package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.iam.application.authentication.LogoutResponse;

/**
 * Presentation DTO for logout response
 */
public record LogoutResponseDto(
    String message,
    String messageKey
) {
    /**
     * Converts from application layer LogoutResponse to presentation DTO
     */
    public static LogoutResponseDto from(LogoutResponse response) {
        return new LogoutResponseDto(
            response.message(),
            response.messageKey()
        );
    }
}
