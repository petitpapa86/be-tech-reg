package com.bcbs239.regtech.core.domain.shared;

/**
 * Small validation helpers that return Maybe instead of nulls to avoid NPEs
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    public static Maybe<String> validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(name.trim());
    }
}