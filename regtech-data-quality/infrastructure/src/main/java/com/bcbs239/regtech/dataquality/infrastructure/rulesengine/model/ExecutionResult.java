package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of rule execution results.
 */
public enum ExecutionResult {
    /**
     * Rule executed successfully without violations
     */
    SUCCESS,
    
    /**
     * Rule executed successfully but detected violations
     */
    FAILURE,
    
    /**
     * Rule execution encountered an error
     */
    ERROR,
    
    /**
     * Rule execution was skipped (e.g., due to exemption)
     */
    SKIPPED
}
