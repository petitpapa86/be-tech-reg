package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of violation resolution statuses.
 */
public enum ResolutionStatus {
    /**
     * Violation is open and unresolved
     */
    OPEN,
    
    /**
     * Violation resolution is in progress
     */
    IN_PROGRESS,
    
    /**
     * Violation has been resolved
     */
    RESOLVED,
    
    /**
     * Violation has been waived/accepted
     */
    WAIVED
}
