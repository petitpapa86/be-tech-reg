package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of parameter types supported by rule parameters.
 */
public enum ParameterType {
    /**
     * Numeric parameter (integer or decimal)
     */
    NUMERIC,
    
    /**
     * Percentage value (0-100)
     */
    PERCENTAGE,
    
    /**
     * Formula or expression
     */
    FORMULA,
    
    /**
     * Conditional expression
     */
    CONDITION,
    
    /**
     * Threshold value
     */
    THRESHOLD,
    
    /**
     * List of values (comma-separated)
     */
    LIST
}
