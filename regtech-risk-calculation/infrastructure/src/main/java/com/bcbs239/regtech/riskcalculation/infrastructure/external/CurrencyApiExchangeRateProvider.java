package com.bcbs239.regtech.riskcalculation.infrastructure.external;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.services.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * CurrencyAPI implementation of ExchangeRateProvider.
 * Integrates with currencyapi.com to fetch real-time and historical exchange rates.
 * 
 * API Documentation: https://currencyapi.com/docs/
 * Features:
 * - Real-time exchange rates
 * - Historical rates
 * - Multiple currency support
 * - Automatic retry on failure
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyApiExchangeRateProvider implements ExchangeRateProvider {
    
    private static final String API_BASE_URL = "https://api.currencyapi.com/v3";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final CurrencyApiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Gets the exchange rate between two currencies for a specific date.
     * 
     * @param fromCurrency The source currency code (e.g., "USD")
     * @param toCurrency The target currency code (e.g., "EUR")
     * @param date The date for the exchange rate
     * @return Result containing the exchange rate or error details
     */
    public Result<ExchangeRate> getRate(String fromCurrency, String toCurrency, LocalDate date) {
        log.debug("Fetching exchange rate from CurrencyAPI: {} to {} on {}", fromCurrency, toCurrency, date);
        
        try {
            // Build API URL
            String url = buildApiUrl(fromCurrency, toCurrency, date);
            
            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .GET()
                .build();
            
            // Execute request with retry logic
            HttpResponse<String> response = executeWithRetry(request);
            
            // Parse response
            if (response.statusCode() == 200) {
                return parseResponse(response.body(), fromCurrency, toCurrency, date);
            } else {
                log.error("CurrencyAPI returned error status: {} - {}", response.statusCode(), response.body());
                return Result.failure(ErrorDetail.of(
                    "CURRENCY_API_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    "CurrencyAPI returned error status: " + response.statusCode(),
                    "currency.api.error"
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch exchange rate from CurrencyAPI", e);
            return Result.failure(ErrorDetail.of(
                "CURRENCY_API_CONNECTION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to connect to CurrencyAPI: " + e.getMessage(),
                "currency.api.connection.error"
            ));
        }
    }
    
    /**
     * Builds the API URL based on whether we need historical or latest rates.
     */
    private String buildApiUrl(String fromCurrency, String toCurrency, LocalDate date) {
        LocalDate today = LocalDate.now();
        
        // Use latest endpoint for today's date, historical for past dates
        if (date.equals(today)) {
            return String.format("%s/latest?apikey=%s&base_currency=%s&currencies=%s",
                API_BASE_URL, properties.getApiKey(), fromCurrency, toCurrency);
        } else {
            String dateStr = date.format(DATE_FORMATTER);
            return String.format("%s/historical?apikey=%s&date=%s&base_currency=%s&currencies=%s",
                API_BASE_URL, properties.getApiKey(), dateStr, fromCurrency, toCurrency);
        }
    }
    
    /**
     * Executes HTTP request with retry logic.
     */
    private HttpResponse<String> executeWithRetry(HttpRequest request) throws Exception {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < properties.getRetryAttempts()) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < properties.getRetryAttempts()) {
                    log.warn("CurrencyAPI request failed (attempt {}/{}), retrying...", 
                        attempts, properties.getRetryAttempts());
                    Thread.sleep(1000 * attempts); // Exponential backoff
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * Parses the CurrencyAPI response and extracts the exchange rate.
     */
    private Result<ExchangeRate> parseResponse(String responseBody, String fromCurrency, 
                                               String toCurrency, LocalDate date) {
        try {
            CurrencyApiResponse apiResponse = objectMapper.readValue(responseBody, CurrencyApiResponse.class);
            
            if (apiResponse.getData() == null || apiResponse.getData().isEmpty()) {
                log.error("CurrencyAPI returned empty data");
                return Result.failure(ErrorDetail.of(
                    "CURRENCY_API_NO_DATA",
                    ErrorType.SYSTEM_ERROR,
                    "CurrencyAPI returned no exchange rate data",
                    "currency.api.no.data"
                ));
            }
            
            // Extract the rate for the target currency
            CurrencyData currencyData = apiResponse.getData().get(toCurrency);
            if (currencyData == null) {
                log.error("CurrencyAPI did not return rate for {}", toCurrency);
                return Result.failure(ErrorDetail.of(
                    "CURRENCY_API_MISSING_RATE",
                    ErrorType.SYSTEM_ERROR,
                    "CurrencyAPI did not return rate for " + toCurrency,
                    "currency.api.missing.rate"
                ));
            }
            
            BigDecimal rate = currencyData.getValue();
            ExchangeRate exchangeRate = new ExchangeRate(rate, fromCurrency, toCurrency, date);
            
            log.debug("Successfully fetched exchange rate from CurrencyAPI: {} {} = 1 {}", 
                rate, toCurrency, fromCurrency);
            
            return Result.success(exchangeRate);
            
        } catch (Exception e) {
            log.error("Failed to parse CurrencyAPI response", e);
            return Result.failure(ErrorDetail.of(
                "CURRENCY_API_PARSE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to parse CurrencyAPI response: " + e.getMessage(),
                "currency.api.parse.error"
            ));
        }
    }
    
    /**
     * CurrencyAPI response structure
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CurrencyApiResponse {
        @JsonProperty("data")
        private Map<String, CurrencyData> data;
    }
    
    /**
     * Currency data structure
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CurrencyData {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("value")
        private BigDecimal value;
    }
}
