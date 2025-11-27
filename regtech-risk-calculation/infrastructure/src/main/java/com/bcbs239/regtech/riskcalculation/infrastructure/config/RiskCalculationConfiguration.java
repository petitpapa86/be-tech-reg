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
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({RiskCalculationProperties.class, CurrencyApiProperties.class})
@EntityScan(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.entities")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories")
public class RiskCalculationConfiguration {

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
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Geographic classifier bean for classifying countries into regions
     */
    @Bean
    public com.bcbs239.regtech.riskcalculation.domain.classification.GeographicClassifier geographicClassifier(
            RiskCalculationProperties properties) {
        String homeCountryCode = properties.getGeographic().getHomeCountry();
        com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country homeCountry =
                com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country.of(homeCountryCode);
        return new com.bcbs239.regtech.riskcalculation.domain.classification.GeographicClassifier(homeCountry);
    }

    /**
     * Sector classifier bean for classifying sectors into categories
     */
    @Bean
    public com.bcbs239.regtech.riskcalculation.domain.classification.SectorClassifier sectorClassifier() {
        return new com.bcbs239.regtech.riskcalculation.domain.classification.SectorClassifier();
    }

    /**
     * Development profile specific configuration
     */
    @Configuration
    @Profile("development")
    static class DevelopmentConfiguration {
        // Development-specific beans can be added here
    }

    /**
     * Production profile specific configuration
     */
    @Configuration
    @Profile("production")
    static class ProductionConfiguration {
        // Production-specific beans can be added here
    }
}