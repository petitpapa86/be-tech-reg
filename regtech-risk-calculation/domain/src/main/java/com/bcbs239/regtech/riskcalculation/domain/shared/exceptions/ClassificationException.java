package com.bcbs239.regtech.riskcalculation.domain.shared.exceptions;

/**
 * Exception thrown when geographic or sector classification fails
 * Indicates issues with mapping exposure data to classification categories
 */
public class ClassificationException extends RiskCalculationDomainException {
    
    public ClassificationException(String message) {
        super(message);
    }
    
    public ClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}