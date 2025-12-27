package com.bcbs239.regtech.dataquality.domain.quality;

import lombok.Getter;

/**
 * Enumeration of the six data quality dimensions as defined by BCBS 239 principles.
 * Each dimension represents a specific aspect of data quality assessment.
 */
@Getter
public enum QualityDimension {
    
    /**
     * Completeness - All required data elements are present and populated
     */
    COMPLETENESS("Completeness", "Completezza", "All required data elements are present and populated"),
    
    /**
     * Accuracy - Data values are correct, valid, and conform to expected formats
     */
    ACCURACY("Accuracy", "Accuratezza", "Data values are correct, valid, and conform to expected formats"),
    
    /**
     * Consistency - Data is consistent across different fields and systems
     */
    CONSISTENCY("Consistency", "Coerenza", "Data is consistent across different fields and systems"),
    
    /**
     * Timeliness - Data is current and available within required timeframes
     */
    TIMELINESS("Timeliness", "Tempestivit\u00e0", "Data is current and available within required timeframes"),
    
    /**
     * Uniqueness - No duplicate records or identifiers exist where they shouldn't
     */
    UNIQUENESS("Uniqueness", "Unicit\u00e0", "No duplicate records or identifiers exist where they shouldn't"),
    
    /**
     * Validity - Data conforms to business rules and domain constraints
     */
    VALIDITY("Validity", "Validit\u00e0", "Data conforms to business rules and domain constraints");

    /**
     * -- GETTER --
     *  Gets the human-readable display name for this dimension
     */
    private final String displayName;

    /**
     * -- GETTER --
     *  Gets the Italian display name for this dimension
     */
    private final String italianName;

    /**
     * -- GETTER --
     *  Gets the description of what this dimension measures
     */
    private final String description;

    QualityDimension(String displayName, String italianName, String description) {
        this.displayName = displayName;
        this.italianName = italianName;
        this.description = description;
    }

    /**
     * Gets the dimension by its display name (case-insensitive)
     */
    public static QualityDimension fromDisplayName(String displayName) {
        for (QualityDimension dimension : values()) {
            if (dimension.displayName.equalsIgnoreCase(displayName)) {
                return dimension;
            }
        }
        throw new IllegalArgumentException("Unknown quality dimension: " + displayName);
    }
}

