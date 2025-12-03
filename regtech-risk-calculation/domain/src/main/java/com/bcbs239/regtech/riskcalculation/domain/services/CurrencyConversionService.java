package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;

/**
 * Domain service interface for currency conversion operations.
 * Provides high-level currency conversion functionality using exchange rates.
 */
public interface CurrencyConversionService {

    /**
     * Convert an amount from one currency to another.
     *
     * @param amount The amount to convert
     * @param fromCurrency The source currency code (e.g., "USD")
     * @param toCurrency The target currency code (e.g., "EUR")
     * @return Result containing the converted amount in EUR
     */
    Result<EurAmount> convertToEur(EurAmount amount, String fromCurrency, String toCurrency);
}