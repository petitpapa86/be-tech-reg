package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;

import java.time.LocalDate;

/**
 * Interface for external exchange rate providers.
 * Allows for different implementations (CurrencyAPI, ECB, commercial providers, etc.)
 * 
 * Implementations should handle:
 * - Real-time and historical exchange rates
 * - Error handling and retries
 * - Rate limiting and caching
 */
public interface ExchangeRateProvider {
    
    /**
     * Gets the exchange rate between two currencies for a specific date.
     * 
     * @param fromCurrency The source currency code (e.g., "USD")
     * @param toCurrency The target currency code (e.g., "EUR")
     * @param date The date for the exchange rate
     * @return Result containing the exchange rate or error details
     */
    Result<ExchangeRate> getRate(String fromCurrency, String toCurrency, LocalDate date);
}
