package com.bcbs239.regtech.billing.domain.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a DunningAction.
 * Provides type safety and prevents mixing up different types of IDs.
 */
public record DunningActionId(String value) {
    
    public DunningActionId {
        Objects.requireNonNull(value, "DunningActionId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("DunningActionId value cannot be empty");
        }
    }
    
    /**
     * Generate a new unique DunningActionId.
     * 
     * @return A new DunningActionId with a UUID value
     */
    public static DunningActionId generate() {
        return new DunningActionId(UUID.randomUUID().toString());
    }
    
    /**
     * Create a DunningActionId from a string value.
     * 
     * @param value The string value to create the ID from
     * @return DunningActionId with the given value
     * @throws IllegalArgumentException if value is null or empty
     */
    public static DunningActionId fromString(String value) {
        return new DunningActionId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}