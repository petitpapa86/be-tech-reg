package com.bcbs239.regtech.billing.domain.dunning;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a DunningCase.
 * Immutable identifier that ensures type safety for dunning case references.
 */
public record DunningCaseId(String value) {
    
    public DunningCaseId {
        Objects.requireNonNull(value, "DunningCaseId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("DunningCaseId value cannot be empty");
        }
    }
    
    /**
     * Generate a new unique DunningCaseId using UUID.
     * 
     * @return New DunningCaseId with unique value
     */
    public static DunningCaseId generate() {
        return new DunningCaseId(UUID.randomUUID().toString());
    }
    
    /**
     * Create DunningCaseId from existing string value.
     * 
     * @param value The string value to create DunningCaseId from
     * @return DunningCaseId with the provided value
     */
    public static DunningCaseId fromString(String value) {
        return new DunningCaseId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}