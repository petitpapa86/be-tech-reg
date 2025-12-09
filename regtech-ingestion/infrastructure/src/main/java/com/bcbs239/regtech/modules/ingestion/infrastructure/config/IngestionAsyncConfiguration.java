package com.bcbs239.regtech.modules.ingestion.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for Ingestion module
 * Creates a dedicated thread pool for async file upload and processing operations
 * Requirements: 13.1, 13.2, 13.5
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(IngestionProperties.class)
public class IngestionAsyncConfiguration {

    private final IngestionProperties properties;

    public IngestionAsyncConfiguration(IngestionProperties properties) {
        this.properties = properties;
        validatePoolSizes();
    }

    /**
     * Creates the ingestion task executor bean
     * Purpose: Handles async file upload and processing operations
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "ingestionTaskExecutor")
    public Executor ingestionTaskExecutor() {
        IngestionProperties.AsyncProperties async = properties.getAsync();
        
        if (!async.isEnabled()) {
            log.warn("Ingestion async processing is disabled");
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

        log.info("Initialized ingestion task executor - core: {}, max: {}, queue: {}", 
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
        IngestionProperties.AsyncProperties async = properties.getAsync();
        
        if (async.getCorePoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Ingestion async core-pool-size must be positive, got: " + async.getCorePoolSize());
        }
        
        if (async.getMaxPoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Ingestion async max-pool-size must be positive, got: " + async.getMaxPoolSize());
        }
        
        if (async.getCorePoolSize() > async.getMaxPoolSize()) {
            throw new IllegalArgumentException(
                String.format("Ingestion async core-pool-size (%d) must be <= max-pool-size (%d)",
                    async.getCorePoolSize(), async.getMaxPoolSize()));
        }
        
        if (async.getQueueCapacity() < 0) {
            throw new IllegalArgumentException(
                "Ingestion async queue-capacity must be non-negative, got: " + async.getQueueCapacity());
        }
    }
}
