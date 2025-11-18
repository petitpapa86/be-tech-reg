package com.bcbs239.regtech.riskcalculation.domain.shared.exceptions;

/**
 * Exception thrown when risk calculation operations fail
 * Indicates issues with mathematical calculations or aggregations
 */
public class CalculationException extends RiskCalculationDomainException {
    
    public CalculationException(String message) {
        super(message);
    }
    
    public CalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}