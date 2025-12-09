package com.bcbs239.regtech.riskcalculation.infrastructure.external;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.http.HttpClient;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for CurrencyApiExchangeRateProvider.
 * Requires CURRENCY_API_KEY environment variable to be set.
 * 
 * Requirements: 2.1, 2.5
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
        
        // When
        ExchangeRate rate = provider.getRate(fromCurrency, toCurrency);
        
        // Then
        assertThat(rate.rate()).isPositive();
        assertThat(rate.fromCurrency()).isEqualTo(fromCurrency);
        assertThat(rate.toCurrency()).isEqualTo(toCurrency);
        assertThat(rate.date()).isEqualTo(LocalDate.now());
        
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
        ExchangeRate rate = provider.getRate(fromCurrency, toCurrency, historicalDate);
        
        // Then
        assertThat(rate.rate()).isPositive();
        assertThat(rate.fromCurrency()).isEqualTo(fromCurrency);
        assertThat(rate.toCurrency()).isEqualTo(toCurrency);
        assertThat(rate.date()).isEqualTo(historicalDate);
        
        System.out.println("Historical exchange rate (" + historicalDate + "): 1 " + 
            fromCurrency + " = " + rate.rate() + " " + toCurrency);
    }
    
    @Test
    void shouldThrowExceptionForInvalidCurrency() {
        // Given
        String fromCurrency = "INVALID";
        String toCurrency = "EUR";
        
        // When/Then - API should throw exception for invalid currency
        assertThatThrownBy(() -> provider.getRate(fromCurrency, toCurrency))
            .isInstanceOf(ExchangeRateUnavailableException.class)
            .hasMessageContaining("INVALID")
            .hasMessageContaining("EUR");
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "CURRENCY_API_KEY", matches = ".*")
    void shouldHandleMultipleCurrencies() {
        // Given
        String[][] currencyPairs = {
            {"USD", "EUR"},
            {"GBP", "EUR"},
            {"JPY", "EUR"},
            {"CHF", "EUR"}
        };
        
        // When/Then
        for (String[] pair : currencyPairs) {
            ExchangeRate rate = provider.getRate(pair[0], pair[1]);
            assertThat(rate.rate()).isPositive();
            assertThat(rate.fromCurrency()).isEqualTo(pair[0]);
            assertThat(rate.toCurrency()).isEqualTo(pair[1]);
            System.out.println("Exchange rate: 1 " + pair[0] + " = " + rate.rate() + " " + pair[1]);
        }
    }
}
