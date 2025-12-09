package com.bcbs239.regtech.riskcalculation.domain.shared.exceptions;

/**
 * Base exception for Risk Calculation domain errors
 * Represents business rule violations and domain-specific errors
 */
public class RiskCalculationDomainException extends RuntimeException {
    
    public RiskCalculationDomainException(String message) {
        super(message);
    }
    
    public RiskCalculationDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}