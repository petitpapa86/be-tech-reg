package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of severity levels for rules and violations.
 */
public enum Severity {
    /**
     * Low severity - informational issues
     */
    LOW,
    
    /**
     * Medium severity - should be addressed
     */
    MEDIUM,
    
    /**
     * High severity - must be addressed soon
     */
    HIGH,
    
    /**
     * Critical severity - immediate action required
     */
    CRITICAL
}
