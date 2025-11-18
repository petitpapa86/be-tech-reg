package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Unique identifier for an individual exposure record
 * Immutable value object that ensures exposure identification consistency
 */
public record ExposureId(String value) {
    
    public ExposureId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ExposureId cannot be null or empty");
        }
    }
    
    public static ExposureId of(String value) {
        return new ExposureId(value);
    }
    
    public static ExposureId generate() {
        return new ExposureId(java.util.UUID.randomUUID().toString());
    }
}