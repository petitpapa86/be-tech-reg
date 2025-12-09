package com.bcbs239.regtech.riskcalculation.infrastructure.external;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Mock implementation of ExchangeRateProvider for development and testing.
 * Returns predefined exchange rates without making external API calls.
 * 
 * This provider is useful for:
 * - Development environments where external API calls are not desired
 * - Testing scenarios requiring predictable exchange rates
 * - Avoiding API rate limits during development
 * 
 * Requirements: 2.1, 2.5
 */
@Slf4j
public class MockExchangeRateProvider implements ExchangeRateProvider {
    
    // Predefined exchange rates (base currency: EUR)
    private static final Map<String, BigDecimal> MOCK_RATES = Map.of(
        "USD", new BigDecimal("1.10"),  // 1 EUR = 1.10 USD
        "GBP", new BigDecimal("0.85"),  // 1 EUR = 0.85 GBP
        "JPY", new BigDecimal("130.00"), // 1 EUR = 130 JPY
        "CHF", new BigDecimal("1.05"),  // 1 EUR = 1.05 CHF
        "CAD", new BigDecimal("1.45"),  // 1 EUR = 1.45 CAD
        "AUD", new BigDecimal("1.60"),  // 1 EUR = 1.60 AUD
        "EUR", BigDecimal.ONE           // 1 EUR = 1 EUR
    );
    
    @Override
    public ExchangeRate getRate(String fromCurrency, String toCurrency) {
        log.debug("Mock: Fetching exchange rate from {} to {}", fromCurrency, toCurrency);
        
        BigDecimal rate = calculateRate(fromCurrency, toCurrency);
        ExchangeRate exchangeRate = new ExchangeRate(rate, fromCurrency, toCurrency, LocalDate.now());
        
        log.debug("Mock: Returning exchange rate: {} {} = 1 {}", rate, toCurrency, fromCurrency);
        return exchangeRate;
    }
    
    /**
     * Calculates the exchange rate between two currencies using predefined rates.
     * All rates are based on EUR, so we convert through EUR if needed.
     * 
     * Formula: rate(FROM -> TO) = rate(EUR -> TO) / rate(EUR -> FROM)
     */
    private BigDecimal calculateRate(String fromCurrency, String toCurrency) {
        // If same currency, return 1
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        // Get rates from EUR to both currencies
        BigDecimal fromRate = MOCK_RATES.getOrDefault(fromCurrency, BigDecimal.ONE);
        BigDecimal toRate = MOCK_RATES.getOrDefault(toCurrency, BigDecimal.ONE);
        
        // Calculate cross rate: FROM -> TO = (EUR -> TO) / (EUR -> FROM)
        return toRate.divide(fromRate, 6, BigDecimal.ROUND_HALF_UP);
    }
}
