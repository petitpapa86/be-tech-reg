package com.bcbs239.regtech.riskcalculation.domain.exposure;

/**
 * Unique identifier for a financial instrument
 * Can represent loan IDs, bond ISINs, derivative contracts, etc.
 * Immutable value object
 */
public record InstrumentId(String value) {
    
    public InstrumentId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("InstrumentId cannot be null or empty");
        }
    }
    
    public static InstrumentId of(String value) {
        return new InstrumentId(value);
    }
}
