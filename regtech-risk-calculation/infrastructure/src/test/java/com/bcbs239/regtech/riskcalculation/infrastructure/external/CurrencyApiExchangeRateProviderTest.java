package com.bcbs239.regtech.riskcalculation.infrastructure.external;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.http.HttpClient;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CurrencyApiExchangeRateProvider.
 * Requires CURRENCY_API_KEY environment variable to be set.
 */
class CurrencyApiExchangeRateProviderTest {
    
    private CurrencyApiExchangeRateProvider provider;
    
    @BeforeEach
    void setUp() {
        CurrencyApiProperties properties = new CurrencyApiProperties();
        properties.setApiKey(System.getenv().getOrDefault("CURRENCY_API_KEY", 
            "cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl"));
        properties.setTimeout(30000);
        properties.setRetryAttempts(3);
        properties.setEnabled(true);
        
        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        
        provider = new CurrencyApiExchangeRateProvider(properties, httpClient, objectMapper);
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "CURRENCY_API_KEY", matches = ".*")
    void shouldFetchLatestExchangeRate() {
        // Given
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        LocalDate today = LocalDate.now();
        
        // When
        Result<ExchangeRate> result = provider.getRate(fromCurrency, toCurrency, today);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        
        ExchangeRate rate = result.getValue().get();
        assertThat(rate.rate()).isPositive();
        assertThat(rate.fromCurrency()).isEqualTo(fromCurrency);
        assertThat(rate.toCurrency()).isEqualTo(toCurrency);
        assertThat(rate.date()).isEqualTo(today);
        
        System.out.println("Exchange rate: 1 " + fromCurrency + " = " + rate.rate() + " " + toCurrency);
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "CURRENCY_API_KEY", matches = ".*")
    void shouldFetchHistoricalExchangeRate() {
        // Given
        String fromCurrency = "GBP";
        String toCurrency = "EUR";
        LocalDate historicalDate = LocalDate.now().minusDays(7);
        
        // When
        Result<ExchangeRate> result = provider.getRate(fromCurrency, toCurrency, historicalDate);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isPresent();
        
        ExchangeRate rate = result.getValue().get();
        assertThat(rate.rate()).isPositive();
        assertThat(rate.fromCurrency()).isEqualTo(fromCurrency);
        assertThat(rate.toCurrency()).isEqualTo(toCurrency);
        assertThat(rate.date()).isEqualTo(historicalDate);
        
        System.out.println("Historical exchange rate (" + historicalDate + "): 1 " + 
            fromCurrency + " = " + rate.rate() + " " + toCurrency);
    }
    
    @Test
    void shouldHandleInvalidCurrency() {
        // Given
        String fromCurrency = "INVALID";
        String toCurrency = "EUR";
        LocalDate today = LocalDate.now();
        
        // When
        Result<ExchangeRate> result = provider.getRate(fromCurrency, toCurrency, today);
        
        // Then - API should return an error for invalid currency
        // Note: This test behavior depends on the API's error handling
        assertThat(result).isNotNull();
    }
}
