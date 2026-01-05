package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.UUID;

/**
 * UserId Value Object - represents a unique user identifier
 */
public record UserId(UUID value) {

    // Public constructor with validation
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId value cannot be null");
        }
    }

    /**
     * Generates a new random UserId
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Creates a UserId from a string with validation
     */
    public static Result<UserId> create(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "USER_ID_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "User ID cannot be null or empty", 
                "validation.user_id_required"
            ));
        }

        try {
            UUID uuid = UUID.fromString(uuidString.trim());
            return Result.success(new UserId(uuid));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "USER_ID_INVALID_FORMAT", 
                ErrorType.VALIDATION_ERROR, 
                "User ID must be a valid UUID format", 
                "validation.user_id_invalid_format"
            ));
        }
    }

    /**
     * Creates a UserId from a string without validation (for internal use)
     * @deprecated Use create() method for validation
     */
    @Deprecated
    public static UserId fromString(String uuidString) {
        return new UserId(UUID.fromString(uuidString));
    }

    public String getValue() {
        return value.toString();
    }

    public UUID getUUID() {
        return value;
    }
}


