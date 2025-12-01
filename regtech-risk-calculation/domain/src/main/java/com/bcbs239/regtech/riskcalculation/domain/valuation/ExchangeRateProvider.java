package com.bcbs239.regtech.riskcalculation.domain.valuation;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;

/**
 * Domain service interface for providing exchange rates
 * Part of the Valuation Engine bounded context
 * 
 * Implementations should handle:
 * - Real-time exchange rate retrieval
 * - Error handling for unavailable rates
 * - Caching and rate limiting
 */
public interface ExchangeRateProvider {
    
    /**
     * Gets the exchange rate from one currency to another
     * 
     * @param fromCurrency The source currency code (e.g., "USD")
     * @param toCurrency The target currency code (e.g., "EUR")
     * @return The exchange rate for conversion
     * @throws ExchangeRateUnavailableException if the rate cannot be retrieved
     */
    ExchangeRate getRate(String fromCurrency, String toCurrency);
}
