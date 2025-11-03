package com.bcbs239.regtech.ingestion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Bank Registry service integration.
 */
@ConfigurationProperties(prefix = "ingestion.bank-registry")
public record BankRegistryProperties(
    String baseUrl,
    int timeout,
    int retryAttempts
) {
    
    public BankRegistryProperties {
        // Set defaults if null
        if (baseUrl == null) baseUrl = "http://localhost:8081";
        if (timeout == 0) timeout = 30000;
        if (retryAttempts == 0) retryAttempts = 3;
    }
}