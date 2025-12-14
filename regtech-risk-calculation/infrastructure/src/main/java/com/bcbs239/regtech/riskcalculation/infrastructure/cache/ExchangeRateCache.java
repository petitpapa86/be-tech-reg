package com.bcbs239.regtech.riskcalculation.infrastructure.cache;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.CurrencyPair;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Session-scoped cache for exchange rates to minimize API calls during batch processing.
 * 
 * This cache is designed to be created once per batch processing session and automatically
 * garbage collected when the session completes. It dramatically reduces exchange rate API
 * calls from potentially millions to just the number of unique currencies in the batch.
 */
public class ExchangeRateCache {
    
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCache.class);
    
    private final Map<CurrencyPair, ExchangeRate> cache;
    private final ExchangeRateProvider provider;
    private final CacheStatistics statistics;
    
    /**
     * Creates a new exchange rate cache.
     * 
     * @param provider the exchange rate provider to fetch rates when not cached
     */
    public ExchangeRateCache(ExchangeRateProvider provider) {
        this.cache = new HashMap<>();
        this.provider = provider;
        this.statistics = new CacheStatistics();
        
        log.debug("Created new ExchangeRateCache");
    }
    
    /**
     * Gets an exchange rate, checking cache first and fetching from provider if needed.
     * 
     * @param fromCurrency the source currency code (e.g., "USD")
     * @param toCurrency the target currency code (e.g., "EUR")
     * @return the exchange rate
     * @throws ExchangeRateUnavailableException if the rate cannot be obtained
     */
    public ExchangeRate getRate(String fromCurrency, String toCurrency) {
        // Handle same currency case
        if (fromCurrency.equals(toCurrency)) {
            statistics.recordHit(); // Consider this a "hit" since no API call needed
            return ExchangeRate.identity();
        }
        
        CurrencyPair pair = CurrencyPair.of(fromCurrency, toCurrency);
        
        // Check cache first
        if (cache.containsKey(pair)) {
            statistics.recordHit();
            ExchangeRate rate = cache.get(pair);
            log.trace("Cache HIT for {}: {}", pair, rate.rate());
            return rate;
        }
        
        // Cache miss - fetch from provider
        statistics.recordMiss();
        log.debug("Cache MISS for {} - fetching from provider", pair);
        
        try {
            ExchangeRate rate = provider.getRate(fromCurrency, toCurrency);
            cache.put(pair, rate);
            log.debug("Cached rate for {}: {}", pair, rate.rate());
            return rate;
        } catch (ExchangeRateUnavailableException e) {
            log.warn("Failed to fetch exchange rate for {}: {}", pair, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Preloads exchange rates for all currencies to a target currency.
     * This is typically called at the start of batch processing to minimize cache misses.
     * 
     * @param currencies the set of source currencies to preload
     * @param targetCurrency the target currency (typically "EUR")
     */
    public void preloadRates(Set<String> currencies, String targetCurrency) {
        log.info("Preloading exchange rates for {} currencies to {}", currencies.size(), targetCurrency);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String currency : currencies) {
            if (!currency.equals(targetCurrency)) {
                try {
                    getRate(currency, targetCurrency);
                    successCount++;
                } catch (ExchangeRateUnavailableException e) {
                    failureCount++;
                    log.warn("Failed to preload rate for {}/{}: {}", currency, targetCurrency, e.getMessage());
                }
            }
        }
        
        log.info("Preloading completed: {} successful, {} failed", successCount, failureCount);
    }
    
    /**
     * Gets the current cache statistics.
     * 
     * @return cache statistics including hit ratio
     */
    public CacheStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Gets the current cache size (number of cached rates).
     * 
     * @return number of cached exchange rates
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Clears the cache and resets statistics.
     * This is optional since the cache will be garbage collected when the session ends.
     */
    public void clear() {
        cache.clear();
        statistics.reset();
        log.debug("Cache cleared");
    }
    
    @Override
    public String toString() {
        return String.format("ExchangeRateCache{size=%d, %s}", 
            getCacheSize(), statistics);
    }
}
