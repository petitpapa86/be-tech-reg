package com.bcbs239.regtech.iam.domain.users;

import java.util.Optional;
import java.util.UUID;

/**
 * UserId Value Object - represents a unique user identifier
 */
public record UserId(UUID value) {

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String uuidString) {
        return new UserId(UUID.fromString(uuidString));
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