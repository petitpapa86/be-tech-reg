package com.bcbs239.regtech.dataquality.application.validation.uniqueness;

/**
 * Defines the types of uniqueness checks performed on exposure data.
 */
public enum UniquenessCheckType {
    
    /**
     * Check 1: ExposureId Univocità
     * Verifies that all exposure IDs are unique within the batch.
     */
    EXPOSURE_ID_UNIQUENESS("ExposureId Univocità", "All exposure IDs must be unique"),
    
    /**
     * Check 2: InstrumentId Univocità
     * Verifies that all instrument IDs are unique within the batch.
     */
    INSTRUMENT_ID_UNIQUENESS("InstrumentId Univocità", "All instrument IDs must be unique"),
    
    /**
     * Check 3: Esposizioni Duplicate (Content-Based)
     * Verifies that no two exposures have identical content (based on hash).
     */
    CONTENT_DUPLICATE("Esposizioni Duplicate", "No duplicate exposure content (content-based hash check)");
    
    private final String displayName;
    private final String description;
    
    UniquenessCheckType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
