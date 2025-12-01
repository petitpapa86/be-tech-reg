package com.bcbs239.regtech.riskcalculation.domain.services;

import java.util.Objects;

/**
 * Value object representing a currency pair for exchange rate caching.
 * Used as a key in the ExchangeRateCache HashMap.
 */
public record CurrencyPair(String from, String to) {
    
    public CurrencyPair {
        Objects.requireNonNull(from, "From currency cannot be null");
        Objects.requireNonNull(to, "To currency cannot be null");
        
        if (from.isBlank()) {
            throw new IllegalArgumentException("From currency cannot be blank");
        }
        if (to.isBlank()) {
            throw new IllegalArgumentException("To currency cannot be blank");
        }
        
        // Normalize to uppercase for consistency
        from = from.trim().toUpperCase();
        to = to.trim().toUpperCase();
    }
    
    /**
     * Factory method to create a CurrencyPair.
     * 
     * @param from the source currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return a new CurrencyPair instance
     */
    public static CurrencyPair of(String from, String to) {
        return new CurrencyPair(from, to);
    }
    
    @Override
    public String toString() {
        return from + "/" + to;
    }
}
