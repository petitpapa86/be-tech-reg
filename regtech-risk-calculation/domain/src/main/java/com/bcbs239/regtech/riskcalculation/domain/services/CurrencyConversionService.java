package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalAmount;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalCurrency;

import java.time.LocalDate;

/**
 * Domain service for currency conversion operations
 * Handles conversion of exposure amounts to EUR using current exchange rates
 * 
 * Requirements: 2.1 - Currency conversion to EUR with rate preservation
 */
public interface CurrencyConversionService {
    
    /**
     * Convert an amount from original currency to EUR
     * 
     * @param amount The original amount to convert
     * @param currency The original currency code
     * @param date The date for which to get the exchange rate
     * @return Result containing the converted EUR amount, or failure if conversion fails
     */
    Result<AmountEur> convertToEur(OriginalAmount amount, OriginalCurrency currency, LocalDate date);
    
    /**
     * Get the exchange rate from a currency to EUR for a specific date
     * 
     * @param fromCurrency The source currency code
     * @param date The date for which to get the exchange rate
     * @return Result containing the exchange rate, or failure if rate is unavailable
     */
    Result<ExchangeRate> getExchangeRate(OriginalCurrency fromCurrency, LocalDate date);
}