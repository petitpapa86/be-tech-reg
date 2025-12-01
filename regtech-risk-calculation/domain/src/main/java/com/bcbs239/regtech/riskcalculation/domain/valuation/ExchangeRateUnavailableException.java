package com.bcbs239.regtech.riskcalculation.domain.valuation;

import lombok.Getter;

/**
 * Exception thrown when an exchange rate cannot be retrieved
 * Part of the Valuation Engine bounded context error handling
 */
@Getter
public class ExchangeRateUnavailableException extends RuntimeException {
    
    private final String fromCurrency;
    private final String toCurrency;
    
    public ExchangeRateUnavailableException(String fromCurrency, String toCurrency) {
        super(String.format("Exchange rate unavailable for %s to %s", fromCurrency, toCurrency));
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }
    
    public ExchangeRateUnavailableException(String fromCurrency, String toCurrency, Throwable cause) {
        super(String.format("Exchange rate unavailable for %s to %s", fromCurrency, toCurrency), cause);
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }

}
