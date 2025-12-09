package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Unique identifier for a batch summary record
 * Immutable value object that ensures batch summary identification consistency
 */
public record BatchSummaryId(String value) {
    
    public BatchSummaryId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BatchSummaryId cannot be null or empty");
        }
    }
    
    public static BatchSummaryId of(String value) {
        return new BatchSummaryId(value);
    }
    
    public static BatchSummaryId generate() {
        return new BatchSummaryId(java.util.UUID.randomUUID().toString());
    }
}