package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.math.BigDecimal;

/**
 * BCBS 239 Compliance Status enumeration
 * Represents the compliance level based on overall quality score
 * Aligned with Basel Committee on Banking Supervision Principles for effective risk data aggregation
 */
public enum ComplianceStatus {
    /**
     * Fully compliant with BCBS 239 principles (score >= 90%)
     */
    COMPLIANT,
    
    /**
     * Largely compliant with minor issues (score >= 75% and < 90%)
     */
    LARGELY_COMPLIANT,
    
    /**
     * Materially non-compliant requiring remediation (score >= 60% and < 75%)
     */
    MATERIALLY_NON_COMPLIANT,
    
    /**
     * Non-compliant requiring immediate action (score < 60%)
     */
    NON_COMPLIANT;
    
    /**
     * Determine compliance status from overall quality score
     * 
     * @param score Overall quality score as percentage (0-100)
     * @return Corresponding compliance status
     */
    public static ComplianceStatus fromScore(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("Quality score cannot be null");
        }
        
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Quality score must be between 0 and 100, got: " + score);
        }
        
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return COMPLIANT;
        } else if (score.compareTo(new BigDecimal("75")) >= 0) {
            return LARGELY_COMPLIANT;
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return MATERIALLY_NON_COMPLIANT;
        } else {
            return NON_COMPLIANT;
        }
    }
    
    /**
     * Check if this status indicates compliance
     */
    public boolean isCompliant() {
        return this == COMPLIANT || this == LARGELY_COMPLIANT;
    }
    
    /**
     * Check if this status requires immediate action
     */
    public boolean requiresImmediateAction() {
        return this == NON_COMPLIANT;
    }
    
    /**
     * Get human-readable description
     */
    public String getDescription() {
        return switch (this) {
            case COMPLIANT -> "Fully compliant with BCBS 239 principles";
            case LARGELY_COMPLIANT -> "Largely compliant with minor issues";
            case MATERIALLY_NON_COMPLIANT -> "Materially non-compliant requiring remediation";
            case NON_COMPLIANT -> "Non-compliant requiring immediate action";
        };
    }
}
