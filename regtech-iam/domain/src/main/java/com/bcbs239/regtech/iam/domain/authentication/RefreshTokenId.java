package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.UUID;

/**
 * RefreshTokenId Value Object - represents a unique refresh token identifier
 */
public record RefreshTokenId(UUID value) {

    public static RefreshTokenId generate() {
        return new RefreshTokenId(UUID.randomUUID());
    }

    public static Result<RefreshTokenId> from(String value) {
        try {
            return Result.success(new RefreshTokenId(UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_REFRESH_TOKEN_ID",
                ErrorType.VALIDATION_ERROR,
                "Invalid refresh token ID format",
                "refresh_token.id.invalid"
            ));
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
