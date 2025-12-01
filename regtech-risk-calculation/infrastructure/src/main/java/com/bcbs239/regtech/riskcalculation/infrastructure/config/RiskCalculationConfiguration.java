package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import com.bcbs239.regtech.riskcalculation.infrastructure.external.CurrencyApiProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration class for Risk Calculation module
 * Provides async processing configuration, thread pool management, and scheduling support
 * Requirements: 2.1, 2.5
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({RiskCalculationProperties.class, CurrencyApiProperties.class})
@EntityScan(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.entities")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories")
public class RiskCalculationConfiguration {

    private final CurrencyApiProperties currencyApiProperties;

    public RiskCalculationConfiguration(CurrencyApiProperties currencyApiProperties) {
        this.currencyApiProperties = currencyApiProperties;
    }

    /**
     * Thread pool executor for risk calculation processing
     * Configured based on application properties
     */
    @Bean("riskCalculationExecutor")
    public Executor riskCalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("RiskCalc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * HTTP client for external API calls (CurrencyAPI)
     * Configured with timeout and redirect settings
     * Requirement 2.1: Configure WebClient for currency API
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(currencyApiProperties.getTimeout()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * ObjectMapper for JSON parsing
     * Used by CurrencyApiExchangeRateProvider
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    // Removed deprecated classifier beans - functionality moved to ExposureClassifier

    /**
     * Async executor for event processing
     * Used by BatchIngestedEventListener for async event handling
     * Requirement 2.1: Set up async executor for event processing
     */
    @Bean("eventProcessingExecutor")
    public Executor eventProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EventProc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Development profile specific configuration
     * Requirement 2.1: Add profile-based configuration
     */
    @Configuration
    @Profile("development")
    static class DevelopmentConfiguration {
        
        /**
         * Development-specific HTTP client with shorter timeouts
         */
        @Bean
        @Profile("development")
        public HttpClient devHttpClient(CurrencyApiProperties properties) {
            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeout() / 2))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        }
    }

    /**
     * Production profile specific configuration
     * Requirement 2.1: Add profile-based configuration
     */
    @Configuration
    @Profile("production")
    static class ProductionConfiguration {
        
        /**
         * Production-specific HTTP client with longer timeouts and connection pooling
         */
        @Bean
        @Profile("production")
        public HttpClient prodHttpClient(CurrencyApiProperties properties) {
            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        }
    }
}