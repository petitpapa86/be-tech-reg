package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

/**
 * Unique identifier for a batch of exposures
 * Immutable value object that ensures batch identification consistency
 * 
 * <p><strong>Shared Value Object:</strong> This value object is used across multiple contexts
 * and modules including ingestion, data quality, risk calculation, and report generation.
 * It represents a fundamental cross-cutting domain concept that identifies a batch of data
 * throughout its lifecycle in the system. Following DDD's "shared kernel" pattern, it resides
 * in the {@code shared/valueobjects} package to enable consistent batch identification across
 * all bounded contexts without creating dependencies between aggregates.</p>
 * 
 * <p>This value object is part of the shared kernel and is essential for tracing data lineage
 * and coordinating processing across multiple modules.</p>
 * 
 * @see com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects
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
