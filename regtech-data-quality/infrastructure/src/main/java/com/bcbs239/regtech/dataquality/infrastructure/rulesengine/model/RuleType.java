package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of rule types supported by the Rules Engine.
 */
public enum RuleType {
    /**
     * Validation rules that check data validity (e.g., format, range, constraints)
     */
    VALIDATION,
    
    /**
     * Threshold rules that check against configurable limits
     */
    THRESHOLD,
    
    /**
     * Calculation rules that compute derived values
     */
    CALCULATION,
    
    /**
     * Business logic rules that implement domain-specific logic
     */
    BUSINESS_LOGIC,
    
    /**
     * Data quality rules that assess data quality dimensions
     */
    DATA_QUALITY
}
