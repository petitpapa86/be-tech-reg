package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.services.ExposureProcessingService;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;

import com.bcbs239.regtech.riskcalculation.infrastructure.external.CurrencyApiExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.infrastructure.external.CurrencyApiProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.external.MockExchangeRateProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
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
 *
 * <p>JPA 3.2 and Hibernate 7.x compatible configuration:
 * <ul>
 *   <li>Supports EntityManager injection with @PersistenceContext and @Inject</li>
 *   <li>Compatible with Hibernate ORM 7.1/7.2</li>
 *   <li>Uses Jakarta Persistence API (jakarta.persistence.*)</li>
 * </ul>
 * <p>
 * Requirements: 2.1, 2.5
 */
@Configuration
//@EnableAsync
//@EnableScheduling
@EnableConfigurationProperties({RiskCalculationProperties.class, CurrencyApiProperties.class})
@EntityScan(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.entities")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories")
@org.springframework.context.annotation.ComponentScan(basePackages = {
        "com.bcbs239.regtech.riskcalculation.application",
        "com.bcbs239.regtech.riskcalculation.infrastructure",
        "com.bcbs239.regtech.riskcalculation.domain",
        "com.bcbs239.regtech.riskcalculation.presentation"
})
@Slf4j
public class RiskCalculationConfiguration {

    private final CurrencyApiProperties currencyApiProperties;
    private final RiskCalculationProperties riskCalculationProperties;

    public RiskCalculationConfiguration(
            CurrencyApiProperties currencyApiProperties,
            RiskCalculationProperties riskCalculationProperties) {
        this.currencyApiProperties = currencyApiProperties;
        this.riskCalculationProperties = riskCalculationProperties;
    }

    /**
     * Exchange rate provider bean - conditionally creates mock or real provider
     * based on configuration
     * Requirement 2.1: Configure exchange rate provider
     */
    @Bean
    public ExchangeRateProvider exchangeRateProvider(HttpClient httpClient, ObjectMapper objectMapper) {
        if (riskCalculationProperties.getCurrency().getProvider().isMockEnabled()) {
            log.info("Using MockExchangeRateProvider for development");
            return new MockExchangeRateProvider();
        } else {
            log.info("Using CurrencyApiExchangeRateProvider for production");
            return new CurrencyApiExchangeRateProvider(currencyApiProperties, httpClient, objectMapper);
        }
    }

    /**
     * Exposure classifier domain service bean.
     * Classifies exposures by geographic region and economic sector.
     * <p>
     * This domain service implements business rules for:
     * - Geographic classification (ITALY, EU_OTHER, NON_EUROPEAN)
     * - Sector classification (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, OTHER)
     * <p>
     * Requirement 4.1-4.4: Geographic classification
     * Requirement 5.1-5.6: Sector classification
     */
    @Bean
    public ExposureClassifier exposureClassifier() {
        log.info("Creating ExposureClassifier domain service");
        return new ExposureClassifier();
    }

    /**
     * Exposure processing domain service bean.
     * Orchestrates the exposure processing pipeline following DDD principles.
     * <p>
     * This domain service coordinates:
     * - Currency conversion to EUR
     * - Credit risk mitigation application
     * - Exposure classification by region and sector
     * <p>
     * Requirement 6.1: Exposure processing pipeline
     * Requirement 7.1: Risk calculation orchestration
     */
    @Bean
    public ExposureProcessingService exposureProcessingService(
            ExchangeRateProvider exchangeRateProvider,
            ExposureClassifier exposureClassifier
    ) {
        log.info("Creating ExposureProcessingService domain service");
        return new ExposureProcessingService(exchangeRateProvider, exposureClassifier);
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

    // ObjectMapper bean removed - using the one from core infrastructure (loggingObjectMapper)

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