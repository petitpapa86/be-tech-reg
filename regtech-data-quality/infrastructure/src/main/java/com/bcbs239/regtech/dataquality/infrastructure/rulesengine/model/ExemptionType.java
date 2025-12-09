package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

/**
 * Enumeration of exemption types.
 */
public enum ExemptionType {
    /**
     * Permanent exemption with no expiration
     */
    PERMANENT,
    
    /**
     * Temporary exemption with a defined expiration date
     */
    TEMPORARY,
    
    /**
     * Conditional exemption that applies only when certain conditions are met
     */
    CONDITIONAL
}
