package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for Risk Calculation module
 * Provides async processing configuration, thread pool management, and scheduling support
 */
@Configuration
@EnableAsync
@EnableScheduling
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
     * Configuration properties for risk calculation module
     */
    @Bean
    @ConfigurationProperties(prefix = "risk-calculation")
    public RiskCalculationProperties riskCalculationProperties() {
        return new RiskCalculationProperties();
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