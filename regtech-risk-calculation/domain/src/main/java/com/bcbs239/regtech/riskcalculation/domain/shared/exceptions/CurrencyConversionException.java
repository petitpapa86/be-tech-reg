package com.bcbs239.regtech.riskcalculation.domain.shared.exceptions;

/**
 * Exception thrown when currency conversion fails
 * Indicates issues with exchange rate retrieval or conversion calculations
 */
public class CurrencyConversionException extends RiskCalculationDomainException {
    
    public CurrencyConversionException(String message) {
        super(message);
    }
    
    public CurrencyConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}