package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.UUID;

/**
 * Bank ID Value Object
 */
public record BankId(String value) {

    // Private constructor to enforce factory method usage
    private BankId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BankId value cannot be null or empty");
        }
    }

    /**
     * Generates a new random BankId
     */
    public static BankId generate() {
        return new BankId(UUID.randomUUID().toString());
    }

    /**
     * Creates a BankId from a string with validation
     */
    public static Result<BankId> create(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_REQUIRED", 
                ErrorType.VALIDATION_ERROR, 
                "Bank ID cannot be null or empty", 
                "validation.bank_id_required"
            ));
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() < 2) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_TOO_SHORT", 
                ErrorType.VALIDATION_ERROR, 
                "Bank ID must be at least 2 characters long", 
                "validation.bank_id_too_short"
            ));
        }

        if (trimmedValue.length() > 50) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_TOO_LONG", 
                ErrorType.VALIDATION_ERROR, 
                "Bank ID cannot exceed 50 characters", 
                "validation.bank_id_too_long"
            ));
        }

        return Result.success(new BankId(trimmedValue));
    }

    /**
     * Creates a BankId from a string with numeric validation
     * This method validates that the BankId can be parsed as a Long
     */
    public static Result<BankId> createNumeric(String value) {
        // First perform standard validation
        Result<BankId> basicValidation = create(value);
        if (basicValidation.isFailure()) {
            return basicValidation;
        }

        BankId bankId = basicValidation.getValue().get();
        
        // Then validate numeric format
        try {
            Long.parseLong(bankId.getValue());
            return Result.success(bankId);
        } catch (NumberFormatException e) {
            return Result.failure(ErrorDetail.of(
                "BANK_ID_INVALID_NUMERIC_FORMAT", 
                ErrorType.VALIDATION_ERROR, 
                "Bank ID must be a valid number", 
                "validation.bank_id_invalid_numeric_format"
            ));
        }
    }

    /**
     * Creates a BankId from a string without validation (for internal use)
     * @deprecated Use create() method for validation
     */
    @Deprecated
    public static Result<BankId> fromString(String value) {
        return create(value);
    }

    public String getValue() {
        return value;
    }

    /**
     * Gets the BankId value as a Long
     * This method assumes the BankId was created with createNumeric() validation
     * @throws NumberFormatException if the value cannot be parsed as Long
     */
    public Long getAsLong() {
        return Long.parseLong(value);
    }
}



