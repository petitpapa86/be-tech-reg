package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Response for successful user registration
 */
public record RegisterUserResponse(UserId userId, String correlationId) {
}