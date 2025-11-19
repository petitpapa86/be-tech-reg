package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

/**
 * Unique identifier for a batch of exposures
 * Immutable value object that ensures batch identification consistency
 */
public record BatchId(@NonNull String value) {
    
    public BatchId {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BatchId cannot be empty");
        }
    }
    
    public static BatchId of(String value) {
        return new BatchId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
