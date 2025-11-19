package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.util.UUID;

/**
 * Unique identifier for generated reports
 * Immutable value object wrapping a UUID
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
