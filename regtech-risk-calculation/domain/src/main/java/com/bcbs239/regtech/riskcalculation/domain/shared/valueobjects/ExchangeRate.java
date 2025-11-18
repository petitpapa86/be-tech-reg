package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange rate for currency conversion
 * Immutable value object that captures rate and date for audit purposes
 */
public record ExchangeRate(BigDecimal rate, LocalDate date) {
    
    public ExchangeRate {
        if (rate == null) {
            throw new IllegalArgumentException("Exchange rate cannot be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        if (date == null) {
            throw new IllegalArgumentException("Exchange rate date cannot be null");
        }
    }
    
    public static ExchangeRate of(BigDecimal rate, LocalDate date) {
        return new ExchangeRate(rate, date);
    }
    
    public static ExchangeRate of(double rate, LocalDate date) {
        return new ExchangeRate(BigDecimal.valueOf(rate), date);
    }
    
    /**
     * Convert an amount using this exchange rate
     */
    public AmountEur convert(BigDecimal originalAmount) {
        return AmountEur.of(originalAmount.multiply(rate));
    }
}