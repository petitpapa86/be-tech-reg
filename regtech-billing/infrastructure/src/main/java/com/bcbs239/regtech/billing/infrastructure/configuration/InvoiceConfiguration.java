package com.bcbs239.regtech.billing.infrastructure.configuration;

import java.util.Currency;

/**
 * Type-safe configuration for invoice settings.
 * Defines invoice due date calculation and currency settings.
 */
public record InvoiceConfiguration(
    int dueDays,
    String currency
) {
    
    /**
     * Gets the number of days from issue date to due date
     */
    public int getDueDays() {
        return dueDays;
    }
    
    /**
     * Gets the default currency for invoices
     */
    public Currency getCurrency() {
        return Currency.getInstance(currency);
    }
    
    /**
     * Gets the currency code as string
     */
    public String getCurrencyCode() {
        return currency;
    }
    
    /**
     * Validates invoice configuration
     */
    public void validate() {
        if (dueDays <= 0) {
            throw new IllegalStateException("Invoice due days must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalStateException("Invoice currency is required");
        }
        
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid currency code: " + currency, e);
        }
        
        // Reasonable business rules
        if (dueDays > 90) {
            throw new IllegalStateException("Invoice due days should not exceed 90 days");
        }
        if (dueDays < 1) {
            throw new IllegalStateException("Invoice due days must be at least 1 day");
        }
    }
}

