package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.util.UUID;

/**
 * Unique identifier for generated reports
 * Immutable value object wrapping a UUID
 * 
 * <p><strong>Shared Value Object:</strong> This value object is used across multiple contexts
 * including the domain layer, application layer, infrastructure layer, and presentation layer.
 * It represents a cross-cutting domain concept that serves as the primary identifier for reports
 * throughout the system. Following DDD's "shared kernel" pattern, it resides in the
 * {@code shared/valueobjects} package to enable reuse across aggregates and layers without
 * creating tight coupling between aggregates.</p>
 * 
 * <p>This value object is part of the shared kernel and should be used whenever a report
 * needs to be uniquely identified across bounded contexts.</p>
 * 
 * @see com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects
 */
public record ReportId(@NonNull UUID value) {
    
    /**
     * Generate a new unique report ID
     */
    public static ReportId generate() {
        return new ReportId(UUID.randomUUID());
    }
    
    /**
     * Create a report ID from an existing UUID
     */
    public static ReportId of(UUID uuid) {
        return new ReportId(uuid);
    }
    
    /**
     * Create a report ID from a string representation
     */
    public static ReportId fromString(String uuidString) {
        return new ReportId(UUID.fromString(uuidString));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
