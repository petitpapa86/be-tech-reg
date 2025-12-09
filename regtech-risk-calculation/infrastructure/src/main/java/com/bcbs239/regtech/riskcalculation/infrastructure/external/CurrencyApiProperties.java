package com.bcbs239.regtech.riskcalculation.infrastructure.external;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for CurrencyAPI integration.
 * Bound from application-risk-calculation.yml under risk-calculation.currency.api
 */
@Data
@Validated
@ConfigurationProperties(prefix = "risk-calculation.currency.api")
public class CurrencyApiProperties {
    
    /**
     * CurrencyAPI API key
     */
    @NotBlank(message = "CurrencyAPI key must be specified")
    private String apiKey;
    
    /**
     * Request timeout in milliseconds
     */
    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    private int timeout = 30000;
    
    /**
     * Number of retry attempts on failure
     */
    @Min(value = 0, message = "Retry attempts must be non-negative")
    private int retryAttempts = 3;
    
    /**
     * Enable/disable the provider (useful for testing)
     */
    private boolean enabled = true;
}
