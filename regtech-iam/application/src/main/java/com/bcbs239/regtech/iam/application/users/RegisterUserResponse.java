package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Response for successful user registration
 */
public record RegisterUserResponse(UserId userId, String correlationId) {
}

