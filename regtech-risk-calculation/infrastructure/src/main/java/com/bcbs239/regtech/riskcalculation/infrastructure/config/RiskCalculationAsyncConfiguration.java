package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for Risk Calculation module
 * Creates a dedicated thread pool for async risk calculation and aggregation operations
 * Requirements: 13.1, 13.2, 13.5
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(RiskCalculationProperties.class)
public class RiskCalculationAsyncConfiguration {

    private final RiskCalculationProperties properties;

    public RiskCalculationAsyncConfiguration(RiskCalculationProperties properties) {
        this.properties = properties;
        validatePoolSizes();
    }

    /**
     * Creates the risk calculation task executor bean
     * Purpose: Handles async risk calculation and aggregation operations
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "riskCalculationTaskExecutor")
    public Executor riskCalculationTaskExecutor() {
        RiskCalculationProperties.AsyncProperties async = properties.getAsync();
        
        if (!async.isEnabled()) {
            log.warn("Risk calculation async processing is disabled");
            return null;
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCorePoolSize());
        executor.setMaxPoolSize(async.getMaxPoolSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setThreadNamePrefix(async.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(async.getAwaitTerminationSeconds());
        executor.initialize();

        log.info("Initialized risk calculation task executor - core: {}, max: {}, queue: {}", 
            async.getCorePoolSize(), 
            async.getMaxPoolSize(), 
            async.getQueueCapacity());

        return executor;
    }

    /**
     * Validates that pool sizes are configured correctly
     * Requirement 13.5: Validate thread pool sizes are positive integers at startup
     */
    private void validatePoolSizes() {
        RiskCalculationProperties.AsyncProperties async = properties.getAsync();
        
        if (async.getCorePoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Risk calculation async core-pool-size must be positive, got: " + async.getCorePoolSize());
        }
        
        if (async.getMaxPoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Risk calculation async max-pool-size must be positive, got: " + async.getMaxPoolSize());
        }
        
        if (async.getCorePoolSize() > async.getMaxPoolSize()) {
            throw new IllegalArgumentException(
                String.format("Risk calculation async core-pool-size (%d) must be <= max-pool-size (%d)",
                    async.getCorePoolSize(), async.getMaxPoolSize()));
        }
        
        if (async.getQueueCapacity() < 0) {
            throw new IllegalArgumentException(
                "Risk calculation async queue-capacity must be non-negative, got: " + async.getQueueCapacity());
        }
    }
}
