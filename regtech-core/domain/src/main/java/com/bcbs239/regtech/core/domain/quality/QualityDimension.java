package com.bcbs239.regtech.core.domain.quality;

/**
 * Enumeration of the six data quality dimensions as defined by BCBS 239 principles.
 * Each dimension represents a specific aspect of data quality assessment.
 * 
 * This is the SHARED version - consolidates duplicates from data-quality and report-generation modules.
 */
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
    TIMELINESS("Timeliness", "Tempestività", "Data is current and available within required timeframes"),
    
    /**
     * Uniqueness - No duplicate records or identifiers exist where they shouldn't
     */
    UNIQUENESS("Uniqueness", "Unicità", "No duplicate records or identifiers exist where they shouldn't"),
    
    /**
     * Validity - Data conforms to business rules and domain constraints
     */
    VALIDITY("Validity", "Validità", "Data conforms to business rules and domain constraints");

    private final String displayName;
    private final String italianName;
    private final String description;

    QualityDimension(String displayName, String italianName, String description) {
        this.displayName = displayName;
        this.italianName = italianName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this dimension
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the Italian display name for this dimension
     */
    public String getItalianName() {
        return italianName;
    }

    /**
     * Gets the description of what this dimension measures
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the localized name for this dimension based on language code
     * @param languageCode "it" for Italian, "en" (or anything else) for English
     */
    public String getLocalizedName(String languageCode) {
        return "it".equalsIgnoreCase(languageCode) ? italianName : displayName;
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
