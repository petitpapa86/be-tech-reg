package com.bcbs239.regtech.dataquality.domain.rules;

/**
 * Enum representing validation rule categories for the Data Quality module.
 * Each category groups related validation rules for better organization and UI presentation.
 * 
 * <p>Categories are used to organize validation rules in the frontend UI,
 * providing a logical grouping for different types of checks.
 * 
 * <p><b>Module:</b> regtech-data-quality/domain
 * <p><b>Layer:</b> Domain (business concepts)
 */
public enum ValidationCategory {
    
    /**
     * Data Quality checks - General quality validations
     * Italian: "Controlli Attivi"
     * Icon: "check-circle"
     */
    DATA_QUALITY("Controlli Attivi", "check-circle"),
    
    /**
     * Numeric Ranges - Validations for numeric value ranges
     * Italian: "Range valori numerici"
     * Icon: "sliders"
     */
    NUMERIC_RANGES("Range valori numerici", "sliders"),
    
    /**
     * Code Validation - Format validation for codes (currency, country, LEI, etc.)
     * Italian: "Validazione codici"
     * Icon: "code"
     */
    CODE_VALIDATION("Validazione codici", "code"),
    
    /**
     * Temporal Coherence - Date and time-related validations
     * Italian: "Coerenza temporale"
     * Icon: "clock"
     */
    TEMPORAL_COHERENCE("Coerenza temporale", "clock"),
    
    /**
     * Duplicate Detection - Checks for duplicate records
     * Italian: "Rilevamento duplicati"
     * Icon: "copy"
     */
    DUPLICATE_DETECTION("Rilevamento duplicati", "copy"),
    
    /**
     * Cross-Reference - Cross-checks between datasets
     * Italian: "Cross-check tra dataset"
     * Icon: "link"
     */
    CROSS_REFERENCE("Cross-check tra dataset", "link");
    
    private final String displayName;
    private final String icon;
    
    ValidationCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getIcon() {
        return icon;
    }
    
    /**
     * Determines the category based on rule code pattern.
     * 
     * @param ruleCode The rule code to categorize
     * @return The appropriate ValidationCategory
     */
    public static ValidationCategory fromRuleCode(String ruleCode) {
        if (ruleCode == null || ruleCode.isBlank()) {
            return DATA_QUALITY; // Default category
        }
        
        String upperCode = ruleCode.toUpperCase();
        
        // Completeness rules → DATA_QUALITY
        if (upperCode.startsWith("COMPLETENESS_")) {
            return DATA_QUALITY;
        }
        
        // Numeric ranges
        if (upperCode.contains("POSITIVE_AMOUNT") || upperCode.contains("REASONABLE_AMOUNT")) {
            return NUMERIC_RANGES;
        }
        
        // Code validation
        if (upperCode.contains("VALID_CURRENCY") || 
            upperCode.contains("VALID_COUNTRY") || 
            upperCode.contains("VALID_LEI") ||
            upperCode.contains("LEI_FORMAT") ||
            upperCode.contains("RATING_FORMAT") ||
            upperCode.contains("VALID_SECTOR")) {
            return CODE_VALIDATION;
        }
        
        // Temporal coherence
        if (upperCode.startsWith("TIMELINESS_") || 
            upperCode.contains("MATURITY") || 
            upperCode.contains("DATE") ||
            upperCode.contains("FUTURE_")) {
            return TEMPORAL_COHERENCE;
        }
        
        // Duplicate detection
        if (upperCode.contains("DUPLICATE") || upperCode.contains("UNIQUE")) {
            return DUPLICATE_DETECTION;
        }
        
        // Cross-reference
        if (upperCode.contains("CROSS") || upperCode.contains("REFERENCE")) {
            return CROSS_REFERENCE;
        }
        
        // Default category
        return DATA_QUALITY;
    }
}
