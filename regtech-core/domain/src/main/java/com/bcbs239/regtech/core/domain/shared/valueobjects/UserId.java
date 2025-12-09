package com.bcbs239.regtech.core.domain.shared.valueobjects;

import java.util.Optional;
import java.util.UUID;

/**
 * UserId Value Object - represents a unique user identifier across all bounded contexts.
 * 
 * This is a shared value object that can be used by any module in the system.
 * It ensures type safety and provides utility methods for working with user identifiers.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId value cannot be null");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId string cannot be null or empty");
        }
        return new UserId(UUID.fromString(uuidString));
    }
    
    public static UserId of(String uuidString) {
        return fromString(uuidString);
    }

    public String getValue() {
        return value.toString();
    }

    public UUID getUUID() {
        return value;
    }

    public Optional<UserId> toOptional() {
        return Optional.of(this);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
