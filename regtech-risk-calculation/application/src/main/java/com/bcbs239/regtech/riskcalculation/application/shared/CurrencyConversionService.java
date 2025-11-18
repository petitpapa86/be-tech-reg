package com.bcbs239.regtech.riskcalculation.application.shared;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalAmount;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.OriginalCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application service for currency conversion with external rate provider integration.
 * Implements the domain CurrencyConversionService interface and provides
 * caching and error handling for currency conversion operations.
 * 
 * Features:
 * - External exchange rate provider integration
 * - Caching for performance optimization
 * - Error handling for unavailable rates
 * - Support for multiple rate sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService implements com.bcbs239.regtech.riskcalculation.domain.services.CurrencyConversionService {
    
    private final ExchangeRateProvider exchangeRateProvider;
    
    // In-memory cache for exchange rates (would use Redis in production)
    private final Map<String, ExchangeRate> rateCache = new ConcurrentHashMap<>();
    
    /**
     * Converts an amount to EUR using current exchange rates.
     * Implements caching to improve performance and reduce external API calls.
     * 
     * @param amount The original amount to convert
     * @param currency The original currency
     * @param date The date for which to get the exchange rate
     * @return Result containing the converted EUR amount or error details
     */
    @Override
    public Result<AmountEur> convertToEur(OriginalAmount amount, OriginalCurrency currency, LocalDate date) {
        log.debug("Converting {} {} to EUR for date: {}", amount.value(), currency.code(), date);
        
        try {
            // If already in EUR, no conversion needed
            if (currency.isEur()) {
                AmountEur eurAmount = AmountEur.from(amount);
                log.debug("Amount already in EUR: {}", eurAmount.value());
                return Result.success(eurAmount);
            }
            
            // Get exchange rate
            Result<ExchangeRate> rateResult = getExchangeRate(currency, date);
            if (rateResult.isFailure()) {
                log.error("Failed to get exchange rate for {} on {}", currency.code(), date);
                return Result.failure(rateResult.getError().get());
            }
            
            ExchangeRate exchangeRate = rateResult.getValue().get();
            
            // Perform conversion
            BigDecimal convertedAmount = amount.value()
                .multiply(exchangeRate.rate())
                .setScale(2, RoundingMode.HALF_UP);
            
            AmountEur eurAmount = new AmountEur(convertedAmount);
            
            log.debug("Converted {} {} to {} EUR using rate {}", 
                amount.value(), currency.code(), eurAmount.value(), exchangeRate.rate());
            
            return Result.success(eurAmount);
            
        } catch (Exception e) {
            log.error("Unexpected error converting {} {} to EUR", amount.value(), currency.code(), e);
            
            return Result.failure(ErrorDetail.of(
                "CURRENCY_CONVERSION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error during currency conversion: " + e.getMessage(),
                "currency.conversion.unexpected.error"
            ));
        }
    }
    
    /**
     * Gets the exchange rate for converting from the given currency to EUR.
     * Implements caching to improve performance.
     * 
     * @param from The source currency
     * @param date The date for which to get the exchange rate
     * @return Result containing the exchange rate or error details
     */
    @Override
    @Cacheable(value = "exchangeRates", key = "#from.code() + '_' + #date")
    public Result<ExchangeRate> getExchangeRate(OriginalCurrency from, LocalDate date) {
        log.debug("Getting exchange rate for {} to EUR on {}", from.code(), date);
        
        try {
            // Check cache first
            String cacheKey = from.code() + "_" + date;
            ExchangeRate cachedRate = rateCache.get(cacheKey);
            if (cachedRate != null) {
                log.debug("Using cached exchange rate for {}: {}", from.code(), cachedRate.rate());
                return Result.success(cachedRate);
            }
            
            // Get rate from external provider
            Result<ExchangeRate> providerResult = exchangeRateProvider.getRate(from.code(), "EUR", date);
            if (providerResult.isFailure()) {
                log.error("Exchange rate provider failed for {} on {}", from.code(), date);
                return providerResult;
            }
            
            ExchangeRate exchangeRate = providerResult.getValue().get();
            
            // Cache the rate
            rateCache.put(cacheKey, exchangeRate);
            
            log.debug("Retrieved and cached exchange rate for {}: {}", from.code(), exchangeRate.rate());
            return Result.success(exchangeRate);
            
        } catch (Exception e) {
            log.error("Unexpected error getting exchange rate for {} on {}", from.code(), date, e);
            
            return Result.failure(ErrorDetail.of(
                "EXCHANGE_RATE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error getting exchange rate: " + e.getMessage(),
                "currency.conversion.rate.error"
            ));
        }
    }
    
    /**
     * Clears the exchange rate cache.
     * Useful for testing or when rates need to be refreshed.
     */
    public void clearCache() {
        rateCache.clear();
        log.info("Exchange rate cache cleared");
    }
    
    /**
     * Gets the current cache size for monitoring.
     */
    public int getCacheSize() {
        return rateCache.size();
    }
}

/**
 * Interface for external exchange rate providers.
 * Allows for different implementations (ECB, commercial providers, etc.)
 */
interface ExchangeRateProvider {
    
    /**
     * Gets the exchange rate between two currencies for a specific date.
     * 
     * @param fromCurrency The source currency code
     * @param toCurrency The target currency code
     * @param date The date for the exchange rate
     * @return Result containing the exchange rate or error details
     */
    Result<ExchangeRate> getRate(String fromCurrency, String toCurrency, LocalDate date);
}