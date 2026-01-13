package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Value Object: Risk Parameters ID
 */
public record RiskParametersId(@NonNull String value) {
    
    public RiskParametersId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Risk Parameters ID cannot be null or empty");
        }
    }
    
    @NonNull
    public static RiskParametersId generate() {
        return new RiskParametersId(UUID.randomUUID().toString());
    }
    
    @NonNull
    public static RiskParametersId of(@NonNull String value) {
        return new RiskParametersId(value);
    }
}
