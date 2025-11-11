package com.bcbs239.regtech.billing.domain.valueobjects;

import java.util.Optional;
import java.util.UUID;

/**
 * UserId Value Object - represents a unique user identifier in the Billing bounded context.
 * 
 * This is a copy of the UserId from the IAM module to maintain bounded context independence.
 * Each module should have its own domain model and not share domain objects across modules.
 * Cross-module references should only use IDs, not full domain objects.
 */
public record UserId(UUID value) {

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String uuidString) {
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
