package com.bcbs239.regtech.riskcalculation.application.shared;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.services.CurrencyConversionService;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application service for currency conversion operations.
 * Implements the domain service interface using the ExchangeRateProvider.
 */
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateProvider exchangeRateProvider;

    @Override
    public Result<EurAmount> convertToEur(EurAmount amount, String fromCurrency, String toCurrency) {
        try {
            if ("EUR".equals(fromCurrency)) {
                return Result.success(amount);
            }

            var exchangeRate = exchangeRateProvider.getRate(fromCurrency, toCurrency);
            var convertedAmount = EurAmount.of(amount.value().multiply(exchangeRate.rate()));

            return Result.success(convertedAmount);
        } catch (Exception e) {
            log.error("Failed to convert {} {} to {}", amount, fromCurrency, toCurrency, e);
            return Result.failure("CURRENCY_CONVERSION_ERROR", ErrorType.SYSTEM_ERROR, e.getMessage(), "currency.conversion.failed");
        }
    }
}