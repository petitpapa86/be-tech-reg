package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Unique identifier for a batch of exposures
 * Immutable value object that ensures batch identification consistency
 */
public record BatchId(String value) {
    
    public BatchId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BatchId cannot be null or empty");
        }
    }
    
    public static BatchId of(String value) {
        return new BatchId(value);
    }
}