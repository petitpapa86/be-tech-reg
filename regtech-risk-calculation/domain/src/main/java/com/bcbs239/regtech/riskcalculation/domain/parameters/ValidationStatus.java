package com.bcbs239.regtech.riskcalculation.domain.parameters;

/**
 * Value Object: Validation Status
 * 
 * Tracks compliance and validation status of risk parameters
 */
public record ValidationStatus(
    boolean bcbs239Compliant,
    boolean capitalUpToDate
) {
    
    public static ValidationStatus createValid() {
        return new ValidationStatus(true, true);
    }
    
    public static ValidationStatus createInvalid() {
        return new ValidationStatus(false, false);
    }
    
    public boolean isValid() {
        return bcbs239Compliant && capitalUpToDate;
    }
}
