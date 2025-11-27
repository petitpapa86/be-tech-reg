package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange rate for currency conversion
 * Immutable value object that captures rate, currencies, and date for audit purposes
 */
public record ExchangeRate(BigDecimal rate, String fromCurrency, String toCurrency, LocalDate date) {
    
    public ExchangeRate {
        if (rate == null) {
            throw new IllegalArgumentException("Exchange rate cannot be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        if (fromCurrency == null || fromCurrency.isBlank()) {
            throw new IllegalArgumentException("From currency cannot be null or blank");
        }
        if (toCurrency == null || toCurrency.isBlank()) {
            throw new IllegalArgumentException("To currency cannot be null or blank");
        }
        if (date == null) {
            throw new IllegalArgumentException("Exchange rate date cannot be null");
        }
    }
    
    public static ExchangeRate of(BigDecimal rate, String fromCurrency, String toCurrency, LocalDate date) {
        return new ExchangeRate(rate, fromCurrency, toCurrency, date);
    }
    
    public static ExchangeRate of(double rate, String fromCurrency, String toCurrency, LocalDate date) {
        return new ExchangeRate(BigDecimal.valueOf(rate), fromCurrency, toCurrency, date);
    }
    
    // Legacy methods for backward compatibility
    public static ExchangeRate of(BigDecimal rate, LocalDate date) {
        return new ExchangeRate(rate, "UNKNOWN", "EUR", date);
    }
    
    public static ExchangeRate of(double rate, LocalDate date) {
        return new ExchangeRate(BigDecimal.valueOf(rate), "UNKNOWN", "EUR", date);
    }
    
    /**
     * Convert an amount using this exchange rate
     */
    public AmountEur convert(BigDecimal originalAmount) {
        return AmountEur.of(originalAmount.multiply(rate));
    }
}